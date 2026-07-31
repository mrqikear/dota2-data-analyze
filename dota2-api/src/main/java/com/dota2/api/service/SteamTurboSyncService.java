package com.dota2.api.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dota2.entity.entity.SteamAccountEntity;
import com.dota2.entity.service.MatchDetailService;
import com.dota2.entity.service.MatchMainService;
import com.dota2.entity.service.MatchPlayerService;
import com.dota2.entity.service.SteamAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dota2.entity.entity.MatchMainEntity;
import com.dota2.entity.entity.MatchPlayerEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import com.dota2.entity.entity.MatchDetailEntity;

/**
 * Sync Turbo mode (game_mode=23) matches.
 * <p>
 * Phase 1 — Discover: GetMatchHistory via Steam China mirror (api.steamchina.com)
 * returns all Turbo match IDs for a given account.
 * <p>
 * Phase 2 — Detail: OpenDota /api/matches/{id} returns full match data (players,
 * KDA, items, etc.). For un-parsed matches, calls OpenDota /api/request to trigger
 * parsing and inserts a minimal match_main skeleton.
 * <p>
 * Dedup: match_main.getById(matchId) skips already-inserted matches.
 */
@Slf4j
@Service
public class SteamTurboSyncService {

    private static final long STEAM64_OFFSET = 76561197960265728L;
    private static final int GAME_MODE_TURBO = 23;

    private static final String OPENDOTA_API = "https://api.opendota.com";
    private static final String OPENDOTA_MATCH_URL = OPENDOTA_API + "/api/matches/{matchId}";
    private static final String OPENDOTA_REQUEST_URL = OPENDOTA_API + "/api/request?match_id={matchId}";
    private static final String HISTORY_URL = "https://api.steamchina.com/IDOTA2Match_570/GetMatchHistory/v1/";

    @Value("${steam.api-key}")
    private String steamApiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MatchMainService matchMainService;

    @Autowired
    private MatchPlayerService matchPlayerService;

    @Autowired
    private MatchDetailService matchDetailService;

    @Autowired
    private SteamAccountService steamAccountService;

    @Autowired
    private MatchMvpService matchMvpService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Periodic scheduled sync: run syncAllAccounts every hour.
     * Uses AtomicBoolean to prevent overlapping runs.
     */
    private final AtomicBoolean scheduledSyncRunning = new AtomicBoolean(false);

    /** Shared backoff: reset hourly when OpenDota returns 429 */
    private volatile long dailyLimitBackoffUntil = 0;

    /** Backoff for per-minute rate limit (from Retry-After header) */
    private volatile long rateLimitBackoffUntil = 0;

    /** Track consecutive daily limit hits for exponential backoff (1h->2h->4h->8h) */
    private volatile int dailyLimitHitCount = 0;

    /** Parse Retry-After header from HttpClientErrorException. Returns seconds, or -1 if not present. */
    private long parseRetryAfter(org.springframework.web.client.HttpClientErrorException e) {
        org.springframework.http.HttpHeaders headers = e.getResponseHeaders();
        if (headers == null) return -1;
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter != null) {
            try { return Long.parseLong(retryAfter.trim()); } catch (NumberFormatException ex) { return -1; }
        }
        return -1;
    }

    @Scheduled(initialDelay = 60000, fixedRate = 3600000)
    public void scheduledSyncAll() {
        if (System.currentTimeMillis() < dailyLimitBackoffUntil) {
            log.warn("[SteamTurbo] scheduled sync skipped: OpenDota daily limit backoff active");
            return;
        }
        if (!scheduledSyncRunning.compareAndSet(false, true)) {
            log.warn("[SteamTurbo] scheduled sync skipped: previous run still in progress");
            return;
        }
        try {
        // Reset dailyLimitHitCount if backoff has expired (allow retry after reset)
        if (System.currentTimeMillis() >= dailyLimitBackoffUntil && dailyLimitBackoffUntil > 0) {
            log.info("[SteamTurbo] daily limit backoff expired, resetting hit count (was {})", dailyLimitHitCount);
            dailyLimitHitCount = 0;
            dailyLimitBackoffUntil = 0;
        }
            log.info("[SteamTurbo] ===== scheduled hourly sync start =====");
            int added = syncAllAccounts();
            log.info("[SteamTurbo] ===== scheduled hourly sync done: {} new matches ====", added);
        } catch (Exception e) {
            log.error("[SteamTurbo] scheduled sync error", e);
        } finally {
            scheduledSyncRunning.set(false);
        }
    }

    // ---- public API ----

    /**
     * Sync Turbo matches filtered by date range.
     * @param minTime unix timestamp, null = no lower bound
     * @param maxTime unix timestamp, null = no upper bound
     */
    public int syncTurboMatches(String steamId, Long minTime, Long maxTime) {
        long accountId = Long.parseLong(steamId) - STEAM64_OFFSET;
        log.info("[SteamTurbo] === start steamId={} accountId={} minTime={} maxTime={}",
                steamId, accountId, minTime, maxTime);

        List<Long> matchIds = discoverTurboMatchIds(accountId, minTime, maxTime);
        log.info("[SteamTurbo] discovered {} Turbo matches in date range", matchIds.size());
        if (matchIds.isEmpty()) return 0;

        int newCount = 0, skipped = 0, pendingParse = 0;
        for (Long matchId : matchIds) {
            if (matchMainService.getById(matchId) != null) { skipped++; continue; }
            try {
                Map<String, Object> detail = fetchFromOpenDota(matchId);
                if (detail != null && !detail.isEmpty()) {
                    insertMatchFromOpenDota(matchId, detail);
                    newCount++;
                } else {
                    requestParse(matchId);
                    insertSkeletonMatch(matchId);
                    pendingParse++;
                }
            } catch (Exception e) {
                log.warn("[SteamTurbo] failed for match {}: {}", matchId, e.getMessage());
                if (System.currentTimeMillis() < dailyLimitBackoffUntil) break;
                if (System.currentTimeMillis() < rateLimitBackoffUntil) break;
            }
            try { Thread.sleep(1100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        try {
            steamAccountService.update(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<SteamAccountEntity>lambdaUpdate()
                            .eq(SteamAccountEntity::getSteamId, steamId)
                            .setSql("last_fetch_time = NOW()"));
        } catch (Exception e) { log.warn("[SteamTurbo] failed to update last_fetch_time: {}", e.getMessage()); }
        log.info("[SteamTurbo] === steamId={} done: new={}, skipped={}, pending_parse={}",
                steamId, newCount, skipped, pendingParse);
        return newCount;
    }

    /**
     * Sync all Turbo matches for a given Steam 64-bit ID.
     *
     * @param steamId 64-bit Steam ID (e.g. "76561198161333880")
     * @return number of new matches inserted into match_main
     */
    public int syncTurboMatches(String steamId) {
        long accountId = Long.parseLong(steamId) - STEAM64_OFFSET;
        log.info("[SteamTurbo] === start steamId={} accountId={}", steamId, accountId);

        // Phase 1: discover all Turbo match IDs for this account via Steam
        List<Long> matchIds = discoverTurboMatchIds(accountId);
        log.info("[SteamTurbo] discovered {} Turbo matches for accountId={}", matchIds.size(), accountId);
        if (matchIds.isEmpty()) return 0;

        // Phase 2: for each new match, fetch detail from OpenDota
        int newCount = 0;
        int skipped = 0;
        int pendingParse = 0;

        for (Long matchId : matchIds) {
            if (matchMainService.getById(matchId) != null) {
                skipped++;
                continue;
            }

            try {
                Map<String, Object> detail = fetchFromOpenDota(matchId);
                if (detail != null && !detail.isEmpty()) {
                    // OpenDota has parsed this match → full insert
                    insertMatchFromOpenDota(matchId, detail);
                    newCount++;
                } else {
                    // OpenDota has NOT parsed this match → request parsing + bare skeleton
                    requestParse(matchId);
                    insertSkeletonMatch(matchId);
                    pendingParse++;
                }
            } catch (Exception e) {
                log.warn("[SteamTurbo] failed for match {}: {}", matchId, e.getMessage());
                if (System.currentTimeMillis() < dailyLimitBackoffUntil) {
                    break; // OpenDota daily limit hit, stop this cycle
                }

                if (System.currentTimeMillis() < rateLimitBackoffUntil) {
                    long waitSec = (rateLimitBackoffUntil - System.currentTimeMillis()) / 1000;
                    log.warn("[SteamTurbo] per-minute limit backoff active ({}s remaining), stopping cycle", waitSec);
                    break;
                }
            }

            // Rate limit: ~1.1s between OpenDota calls
            try { Thread.sleep(1100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        // Mark this account as synced
        try {
            steamAccountService.update(
                    Wrappers.<SteamAccountEntity>lambdaUpdate()
                            .eq(SteamAccountEntity::getSteamId, steamId)
                            .setSql("last_fetch_time = NOW()")
            );
        } catch (Exception e) {
            log.warn("[SteamTurbo] failed to update last_fetch_time: {}", e.getMessage());
        }

        log.info("[SteamTurbo] === steamId={} done: new={}, skipped(dup)={}, pending_parse={}",
                steamId, newCount, skipped, pendingParse);
        return newCount;
    }

    /**
     * Sync Turbo mode for ALL tracked Steam accounts.
     */
    public int syncAllAccounts() {
        List<SteamAccountEntity> accounts = steamAccountService.list();
        log.info("[SteamTurbo] syncAll: {} accounts", accounts.size());

        // Fetch all Turbo match IDs from Steam for all accounts first
        // (avoids duplicate cross-account match IDs)
        // Then dedup globally before fetching detail
        java.util.Set<Long> globalMatchIds = new java.util.LinkedHashSet<>();
        for (SteamAccountEntity a : accounts) {
            long accountId = Long.parseLong(a.getSteamId()) - STEAM64_OFFSET;
            List<Long> ids = discoverTurboMatchIds(accountId);
            log.debug("[SteamTurbo] account {} returned {} matches", a.getSteamId(), ids.size());
            globalMatchIds.addAll(ids);
        }

        log.info("[SteamTurbo] syncAll: {} unique match IDs across {} accounts", globalMatchIds.size(), accounts.size());

        int total = 0;
        int skipped = 0;
        int pendingParse = 0;

        for (Long matchId : globalMatchIds) {
            if (matchMainService.getById(matchId) != null) {
                skipped++;
                continue;
            }

            try {
                Map<String, Object> detail = fetchFromOpenDota(matchId);
                if (detail != null && !detail.isEmpty()) {
                    insertMatchFromOpenDota(matchId, detail);
                    total++;
                } else {
                    requestParse(matchId);
                    insertSkeletonMatch(matchId);
                    pendingParse++;
                }
            } catch (Exception e) {
                log.warn("[SteamTurbo] syncAll: failed for match {}: {}", matchId, e.getMessage());
                if (System.currentTimeMillis() < dailyLimitBackoffUntil) {
                    break; // OpenDota daily limit hit, stop this cycle
                }

                if (System.currentTimeMillis() < rateLimitBackoffUntil) {
                    long waitSec = (rateLimitBackoffUntil - System.currentTimeMillis()) / 1000;
                    log.warn("[SteamTurbo] per-minute limit backoff active ({}s remaining), stopping cycle", waitSec);
                    break;
                }
            }

            try { Thread.sleep(1100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        log.info("[SteamTurbo] syncAll done: new={}, skipped={}, pending_parse={}", total, skipped, pendingParse);
        return total;
    }

    // ---- Steam GetMatchHistory (discovery) ----

    /** Discover Turbo match IDs with their start_time. Returns map matchId → startTime. */
    private java.util.Map<Long, Long> discoverTurboMatchIdsWithTime(long accountId) {
        java.util.Map<Long, Long> resultMap = new java.util.LinkedHashMap<>();
        Long cursor = null;

        while (true) {
            String url = buildHistoryUrl(accountId, cursor);
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                if (resp == null) break;

                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) resp.get("result");
                if (result == null || !Integer.valueOf(1).equals(result.get("status"))) {
                    log.warn("[SteamTurbo] GetMatchHistory bad status, stopping");
                    break;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> matches =
                        (List<Map<String, Object>>) result.get("matches");
                if (matches == null || matches.isEmpty()) break;

                for (Map<String, Object> m : matches) {
                    Long matchId = toLong(m.get("match_id"));
                    Long startTime = toLong(m.get("start_time"));
                    if (matchId != null) resultMap.put(matchId, startTime);
                }

                int remaining = toInt(result.get("results_remaining"));
                log.debug("[SteamTurbo] page: got {} matches, {} remaining", matches.size(), remaining);
                if (remaining <= 0) break;

                Map<String, Object> lastMatch = matches.get(matches.size() - 1);
                cursor = toLong(lastMatch.get("match_id"));
                Thread.sleep(500);
            } catch (Exception e) {
                log.warn("[SteamTurbo] fetch page error: {}", e.getMessage());
                break;
            }
        }
        return resultMap;
    }

    /** Legacy wrapper: returns just match IDs (no date filter, full pull). */
    private List<Long> discoverTurboMatchIds(long accountId) {
        return new ArrayList<>(discoverTurboMatchIdsWithTime(accountId).keySet());
    }

    /** Discover Turbo matches filtered by date range (unix timestamps). */
    private List<Long> discoverTurboMatchIds(long accountId, Long minTime, Long maxTime) {
        java.util.Map<Long, Long> all = discoverTurboMatchIdsWithTime(accountId);
        List<Long> filtered = new ArrayList<>();
        for (java.util.Map.Entry<Long, Long> entry : all.entrySet()) {
            Long time = entry.getValue();
            if (time == null) continue;
            if (minTime != null && time < minTime) continue;
            if (maxTime != null && time > maxTime) continue;
            filtered.add(entry.getKey());
        }
        return filtered;
    }

    private String buildHistoryUrl(long accountId, Long startAtMatchId) {
        StringBuilder sb = new StringBuilder(HISTORY_URL)
                .append("?key=").append(steamApiKey)
                .append("&game_mode=").append(GAME_MODE_TURBO)
                .append("&account_id=").append(accountId)
                .append("&matches_requested=100");
        if (startAtMatchId != null) {
            sb.append("&start_at_match_id=").append(startAtMatchId);
        }
        return sb.toString();
    }

    // ---- OpenDota detail fetch ----

    /**
     * Fetch match detail from OpenDota /api/matches/{id}.
     * Returns a Map with parsed result fields, or null if the match hasn't been parsed.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchFromOpenDota(long matchId) {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(OPENDOTA_MATCH_URL, String.class, matchId);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            // Check if the match actually has player data (parsed)
            if (root.path("players").isMissingNode() || !root.path("players").isArray()
                    || root.path("players").size() == 0) {
                return null; // not parsed by OpenDota
            }
            // Flatten root fields into a Map for backward compatibility with player-insertion logic
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("radiant_win", root.path("radiant_win").asBoolean(false));
            map.put("start_time", root.path("start_time").asLong(0));
            map.put("duration", root.path("duration").asInt(0));
            map.put("game_mode", root.path("game_mode").asInt(0));
            map.put("lobby_type", root.path("lobby_type").asInt(0));
            map.put("radiant_score", root.path("radiant_score").asInt(0));
            map.put("dire_score", root.path("dire_score").asInt(0));
            map.put("players", root.path("players"));
            map.put("_rawJson", resp.getBody()); // raw JSON for match_detail
            return map;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getRawStatusCode() == 404) {
                return null; // not parsed by OpenDota
            }
            if (e.getRawStatusCode() == 429) {
                long retryAfter = parseRetryAfter(e);
                if (retryAfter > 0 && retryAfter < 300) {
                    // Per-minute rate limit: use Retry-After directly (usually 10~60s)
                    rateLimitBackoffUntil = System.currentTimeMillis() + retryAfter * 1000;
                    log.warn("[SteamTurbo] OpenDota per-minute limit (429), retry after {}s", retryAfter);
                } else {
                    // Daily limit: exponential backoff 1h->2h->4h->8h (cap at 8h)
                    dailyLimitHitCount = Math.min(dailyLimitHitCount + 1, 4);
                    long backoffSeconds = (long) (3600 * Math.pow(2, dailyLimitHitCount - 1));
                    dailyLimitBackoffUntil = System.currentTimeMillis() + backoffSeconds * 1000;
                    log.warn("[SteamTurbo] OpenDota daily limit (429, hit #{}) backing off {}h (until {})",
                            dailyLimitHitCount, backoffSeconds / 3600,
                            new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(dailyLimitBackoffUntil)));
                }
                throw e; // let caller know it's a rate limit
            }
            log.warn("[SteamTurbo] OpenDota HTTP error for match {}: {}", matchId, e.getRawStatusCode());
            return null;
        } catch (Exception e) {
            log.warn("[SteamTurbo] OpenDota fetch error for match {}: {}", matchId, e.getMessage());
            return null;
        }
    }

    /**
     * Request OpenDota to parse a match.
     */
    private void requestParse(long matchId) {
        try {
            restTemplate.getForEntity(OPENDOTA_REQUEST_URL, String.class, matchId);
            log.info("[SteamTurbo] requested OpenDota parse for match_id={}", matchId);
        } catch (Exception e) {
            log.warn("[SteamTurbo] requestParse failed for match {}: {}", matchId, e.getMessage());
        }
    }

    // ---- DB insert helpers ----

    /**
     * Insert a full match (main + players) from OpenDota parsed data.
     */
    private void insertMatchFromOpenDota(Long matchId, Map<String, Object> detail) {
        boolean radiantWin = Boolean.TRUE.equals(detail.get("radiant_win"));
        long startTime = detail.get("start_time") instanceof Number
                ? ((Number) detail.get("start_time")).longValue() : 0L;
        int duration = toInt(detail.get("duration"));
        int gameMode = toInt(detail.get("game_mode"));
        int lobbyType = toInt(detail.get("lobby_type"));
        int radiantScore = toInt(detail.get("radiant_score"));
        int direScore = toInt(detail.get("dire_score"));
        String rawJson = (String) detail.get("_rawJson");

        // Insert match_main
        MatchMainEntity mm = new MatchMainEntity();
        mm.setMatchId(matchId);
        mm.setStartTime(startTime);
        mm.setDuration(duration);
        mm.setGameMode(gameMode > 0 ? gameMode : GAME_MODE_TURBO);
        mm.setLobbyType(lobbyType);
        mm.setCreatedTime(LocalDateTime.now());
        matchMainService.save(mm);

        // Insert match_detail
        if (rawJson != null) {
            try {
                MatchDetailEntity md = new MatchDetailEntity();
                md.setMatchId(matchId);
                md.setRadiantWin(radiantWin);
                md.setDuration(duration);
                md.setGameMode(gameMode);
                md.setLobbyType(lobbyType);
                md.setRadiantScore(radiantScore);
                md.setDireScore(direScore);
                md.setRawJson(rawJson);
                md.setSyncStatus(2); // done
                md.setSyncError("");
                md.setCreatedTime(LocalDateTime.now());
                md.setUpdatedTime(LocalDateTime.now());
                matchDetailService.save(md);
            } catch (Exception e) {
                log.warn("[SteamTurbo] match_detail insert failed for {}: {}", matchId, e.getMessage());
            }
        }

        // Insert match_player for all 10 players
        Object playersObj = detail.get("players");
        if (playersObj instanceof JsonNode) {
            JsonNode players = (JsonNode) playersObj;
            for (JsonNode p : players) {
                Long playerAccountId = p.has("account_id") && !p.path("account_id").isNull()
                        ? p.path("account_id").asLong(0) : null;
                if (playerAccountId == null || playerAccountId == 0) continue;
                long playerSteamId = playerAccountId + STEAM64_OFFSET;

                MatchPlayerEntity mp = new MatchPlayerEntity();
                mp.setMatchId(matchId);
                mp.setSteamId(String.valueOf(playerSteamId));
                mp.setHeroId(p.path("hero_id").asInt(0));

                mp.setKills(p.path("kills").asInt(0));
                mp.setDeaths(p.path("deaths").asInt(0));
                mp.setAssists(p.path("assists").asInt(0));

                int playerSlot = p.path("player_slot").asInt(0);
                boolean isRadiant = playerSlot < 128;
                mp.setWin(isRadiant == radiantWin);

                mp.setGoldPerMin(p.path("gold_per_min").asInt(0));
                mp.setXpPerMin(p.path("xp_per_min").asInt(0));
                mp.setLastHits(p.path("last_hits").asInt(0));
                mp.setDenies(p.path("denies").asInt(0));
                mp.setHeroDamage(p.path("hero_damage").asInt(0));
                mp.setTowerDamage(p.path("tower_damage").asInt(0));
                mp.setHeroHealing(p.path("hero_healing").asInt(0));
                mp.setCreatedTime(LocalDateTime.now());

                try {
                    matchPlayerService.save(mp);
                } catch (Exception e) {
                    log.debug("[SteamTurbo] dup match_player: matchId={} steamId={}", matchId, playerSteamId);
                }
            }
        }

        // Calculate MVP/FMVP and persist to match_main
        try { matchMvpService.calculateAndSave(matchId); } catch (Exception e) {
            log.debug("[SteamTurbo] mvp calc for {}: {}", matchId, e.getMessage());
        }

        log.info("[SteamTurbo] inserted match_id={} (game_mode={}, duration={}s, players={})",
                matchId, gameMode, duration,
                playersObj instanceof JsonNode ? ((JsonNode) playersObj).size() : 0);
    }

    /**
     * Insert a bare-minimum match_main record when OpenDota hasn't parsed yet.
     * Only inserts if not exists — never overwrites existing data.
     */
    private void insertSkeletonMatch(Long matchId) {
        try {
            if (matchMainService.getById(matchId) != null) return;
            MatchMainEntity mm = new MatchMainEntity();
            mm.setMatchId(matchId);
            mm.setGameMode(GAME_MODE_TURBO);
            mm.setCreatedTime(LocalDateTime.now());
            matchMainService.save(mm);
            log.info("[SteamTurbo] inserted skeleton match_main for match_id={} (awaiting parse)", matchId);
        } catch (Exception e) {
            log.warn("[SteamTurbo] skeleton insert failed for match {}: {}", matchId, e.getMessage());
        }
    }

    // ---- helpers ----

    private static int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
    }

    private static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }
}
