package com.dota2.api.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dota2.entity.dao.MatchDetailDao;
import com.dota2.entity.entity.MatchDetailEntity;
import com.dota2.entity.entity.MatchPlayerEntity;
import com.dota2.entity.entity.SteamAccountEntity;
import com.dota2.entity.service.MatchDetailService;
import com.dota2.entity.service.MatchMainService;
import com.dota2.entity.service.MatchPlayerService;
import com.dota2.entity.service.SteamAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchDetailSyncService {

    private static final String OPENDOTA_API = "https://api.opendota.com";
    private static final String OPENDOTA_MATCH_URL = OPENDOTA_API + "/api/matches/{matchId}";
    private static final String OPENDOTA_REQUEST_URL = OPENDOTA_API + "/api/request?match_id={matchId}";

    /** Backoff until this millis when OpenDota returns 429 (daily limit). Resets hourly. */
    private volatile long dailyLimitBackoffUntil = 0;

    /** Backoff for per-minute rate limit (from Retry-After header) */
    private volatile long rateLimitBackoffUntil = 0;

    /** Track consecutive daily limit hits for exponential backoff (1h->2h->4h->8h) */
    private volatile int dailyLimitHitCount = 0;

    /** Parse Retry-After header from HttpClientErrorException. Returns seconds, or -1. */
    private long parseRetryAfter(org.springframework.web.client.HttpClientErrorException e) {
        org.springframework.http.HttpHeaders headers = e.getResponseHeaders();
        if (headers == null) return -1;
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter != null) {
            try { return Long.parseLong(retryAfter.trim()); } catch (NumberFormatException ex) { return -1; }
        }
        return -1;
    }

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MatchDetailDao matchDetailDao;

    @Autowired
    private MatchDetailService matchDetailService;

    @Autowired
    private MatchMainService matchMainService;

    @Autowired
    private MatchPlayerService matchPlayerService;

    @Autowired
    private SteamAccountService steamAccountService;

    @Autowired
    private MatchMvpService matchMvpService;

    private static final long API_CALL_MIN_INTERVAL_MS = 1100;

    private final java.util.concurrent.atomic.AtomicBoolean syncing = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Scheduled task: every 10 seconds (fixedRate), scan for match IDs that don't have details
     * and fetch them from OpenDota. Uses AtomicBoolean to prevent overlap.
     */
    @Scheduled(initialDelay = 60000, fixedRate = 10000)
    public void syncMissingDetails() {
        // Skip when OpenDota daily limit backoff is active
        // Reset dailyLimitHitCount if backoff has expired
        if (System.currentTimeMillis() >= dailyLimitBackoffUntil && dailyLimitBackoffUntil > 0) {
            log.info("[MatchDetail] daily limit backoff expired, resetting hit count (was {})", dailyLimitHitCount);
            dailyLimitHitCount = 0;
            dailyLimitBackoffUntil = 0;
        }
        // Skip if per-minute rate limit backoff is active
        if (System.currentTimeMillis() < rateLimitBackoffUntil) {
            log.debug("[MatchDetail] per-minute limit backoff active, skipping");
            return;
        }
        if (System.currentTimeMillis() < dailyLimitBackoffUntil) {
            return;
        }

        // Guard against overlap (previous run still in progress)
        if (!syncing.compareAndSet(false, true)) {
            log.debug("syncMissingDetails: previous run still in progress, skipping this cycle");
            return;
        }
        try {
            doSyncMissingDetails();
        } finally {
            syncing.set(false);
        }
    }

    private void doSyncMissingDetails() {
        // Dynamic batch size: process up to 10 per run when there's a backlog
        int batchSize = getPendingCount();
        if (batchSize <= 0) {
            return;
        }

        List<Long> missingIds = matchDetailDao.selectMissingDetailIds(batchSize);
        if (missingIds.isEmpty()) {
            return;
        }
        log.info("syncMissingDetails: found {} match IDs without details (batch={}), fetching...", missingIds.size(), batchSize);

        for (int i = 0; i < missingIds.size(); i++) {
            long t0 = System.currentTimeMillis();
            Long matchId = missingIds.get(i);

            try {
                fetchAndSaveDetail(matchId);
            } catch (Exception e) {
                log.warn("syncMissingDetails: failed for match {}: {}", matchId, e.getMessage());

                // Mark as failed so we don't retry indefinitely
                updateFailRecord(matchId, e.getMessage());
            }

            // Adaptive rate limit: ensure at least 1.1s between API calls
            long elapsed = System.currentTimeMillis() - t0;
            long sleepMs = API_CALL_MIN_INTERVAL_MS - elapsed;
            if (sleepMs > 0 && i < missingIds.size() - 1) {
                try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private int getPendingCount() {
        // Dynamically size batch: more pending = larger batch
        long total = matchDetailDao.countMissingDetailIds();
        if (total <= 0) return 0;
        // more pending = larger batch (5 ~ 20)
        return (int) Math.min(Math.max(total, 5), 20);
    }

    private void updateFailRecord(Long matchId, String errMsg) {
        try {
            MatchDetailEntity failRecord = new MatchDetailEntity();
            failRecord.setMatchId(matchId);
            failRecord.setSyncStatus(-1);
            failRecord.setSyncError(truncate(errMsg, 500));
            failRecord.setUpdatedTime(LocalDateTime.now());
            matchDetailService.saveOrUpdate(failRecord, Wrappers.<MatchDetailEntity>lambdaUpdate()
                    .eq(MatchDetailEntity::getMatchId, matchId));
        } catch (Exception ignored) {}
    }

    /**
     * Fetch match detail from OpenDota /api/matches/{id} and save to match_detail.
     */
    @SuppressWarnings("unchecked")
    public boolean fetchAndSaveDetail(Long matchId) {
        log.info("fetching match detail for match_id={}", matchId);

        // Mark as in-progress (upsert 鈥?already exists if skeleton was created during sync)
        MatchDetailEntity existingMd = matchDetailService.getById(matchId);
        if (existingMd != null) {
            existingMd.setSyncStatus(1);
            existingMd.setUpdatedTime(LocalDateTime.now());
            matchDetailService.updateById(existingMd);
        } else {
            matchDetailDao.insert(new MatchDetailEntity() {{
                setMatchId(matchId);
                setSyncStatus(1);
                setCreatedTime(LocalDateTime.now());
                setUpdatedTime(LocalDateTime.now());
            }});
        }

        try {
            // Call OpenDota API
            ResponseEntity<String> resp = restTemplate.getForEntity(
                    OPENDOTA_MATCH_URL, String.class, matchId);

            if (resp.getStatusCodeValue() == 429) {
                // Try reading Retry-After header directly from response (no exception context here)
                long retryAfter = -1;
                String retryAfterStr = resp.getHeaders().getFirst("Retry-After");
                if (retryAfterStr != null) {
                    try { retryAfter = Long.parseLong(retryAfterStr.trim()); } catch (NumberFormatException ignored) {}
                }
                if (retryAfter > 0 && retryAfter < 300) {
                    // Per-minute rate limit: use Retry-After directly
                    rateLimitBackoffUntil = System.currentTimeMillis() + retryAfter * 1000;
                    log.warn("match_id={} OpenDota per-minute limit (429), retry after {}s", matchId, retryAfter);
                } else {
                    // Daily limit: exponential backoff 1h->2h->4h->8h
                    dailyLimitHitCount = Math.min(dailyLimitHitCount + 1, 4);
                    long backoffSeconds = (long) (3600 * Math.pow(2, dailyLimitHitCount - 1));
                    dailyLimitBackoffUntil = System.currentTimeMillis() + backoffSeconds * 1000;
                    log.warn("match_id={} OpenDota daily limit (429, hit #{}), backing off {}h (until {})",
                            matchId, dailyLimitHitCount, backoffSeconds / 3600,
                            new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(dailyLimitBackoffUntil)));
                }
                matchDetailDao.deleteById(matchId);
                return false;
            }

            if (resp.getStatusCodeValue() == 404) {
                // Match not yet parsed by OpenDota 鈫?request parse + remove temp record so next cycle retries
                log.info("match_id={} not yet parsed by OpenDota, requesting parse...", matchId);
                try {
                    restTemplate.getForEntity(OPENDOTA_REQUEST_URL, String.class, matchId);
                } catch (Exception ignored) {}
                matchDetailDao.deleteById(matchId);
                return false;
            }

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("OpenDota returned " + resp.getStatusCode());
            }

            String rawJson = resp.getBody();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            // Extract key fields
            boolean radiantWin = root.path("radiant_win").asBoolean(false);
            long startTime = root.path("start_time").asLong(0);
            int duration = root.path("duration").asInt(0);
            int gameMode = root.path("game_mode").asInt(0);
            int lobbyType = root.path("lobby_type").asInt(0);
            int radiantScore = root.path("radiant_score").asInt(0);
            int direScore = root.path("dire_score").asInt(0);
            String radiantName = root.path("radiant_name").asText("");
            String direName = root.path("dire_name").asText("");
            int firstBloodTime = root.path("first_blood_time").asInt(0);

            // Update match_detail
            MatchDetailEntity detail = new MatchDetailEntity();
            detail.setMatchId(matchId);
            detail.setRadiantWin(radiantWin);
            detail.setDuration(duration);
            detail.setGameMode(gameMode);
            detail.setLobbyType(lobbyType);
            detail.setRadiantScore(radiantScore);
            detail.setDireScore(direScore);
            detail.setRadiantName(radiantName);
            detail.setDireName(direName);
            detail.setFirstBloodTime(firstBloodTime);
            detail.setRawJson(rawJson);
            detail.setSyncStatus(2);
            detail.setSyncError("");
            detail.setUpdatedTime(LocalDateTime.now());

            matchDetailService.update(detail, Wrappers.<MatchDetailEntity>lambdaUpdate()
                    .eq(MatchDetailEntity::getMatchId, matchId));

            // Also refresh the match_main fields if they were empty
            matchMainService.lambdaUpdate()
                    .eq(com.dota2.entity.entity.MatchMainEntity::getMatchId, matchId)
                    .set(com.dota2.entity.entity.MatchMainEntity::getStartTime, startTime)
                    .set(com.dota2.entity.entity.MatchMainEntity::getDuration, duration)
                    .set(com.dota2.entity.entity.MatchMainEntity::getGameMode, gameMode)
                    .set(com.dota2.entity.entity.MatchMainEntity::getLobbyType, lobbyType)
                    .update();

            // Insert match_player records from OpenDota player data
            JsonNode players = root.path("players");
            int playerCount = 0;
            if (players.isArray()) {
                for (JsonNode p : players) {
                    Long playerAccountId = p.has("account_id") && !p.path("account_id").isNull()
                            ? p.path("account_id").asLong(0) : null;
                    if (playerAccountId == null || playerAccountId == 0) continue;
                    long playerSteamId = playerAccountId + 76561197960265728L;

                    int playerSlot = p.path("player_slot").asInt(0);
                    boolean isRadiant = playerSlot < 128;

                    MatchPlayerEntity mp = new MatchPlayerEntity();
                    mp.setMatchId(matchId);
                    mp.setSteamId(String.valueOf(playerSteamId));
                    mp.setHeroId(p.path("hero_id").asInt(0));
                    mp.setKills(p.path("kills").asInt(0));
                    mp.setDeaths(p.path("deaths").asInt(0));
                    mp.setAssists(p.path("assists").asInt(0));
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
                        playerCount++;
                    } catch (Exception dup) {
                        // Unique constraint (match_id, steam_id) 鈫?already exists, skip
                        log.debug("match_player dup: matchId={} steamId={}", matchId, playerSteamId);
                    }
                }
            }

            // Calculate MVP/FMVP and persist to match_main
            try { matchMvpService.calculateAndSave(matchId); } catch (Exception e) {
                log.debug("[MatchDetail] mvp calc for {}: {}", matchId, e.getMessage());
            }

            log.info("match_id={} detail saved OK (radiant_win={}, players={})", matchId, radiantWin, playerCount);
            return true;

        } catch (Exception e) {
            log.warn("match_id={} fetch failed: {}", matchId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Force fetch details for a specific match (used by the controller).
     */
    public boolean fetchAndSaveDetailSync(Long matchId) {
        try {
            // Check if already synced
            MatchDetailEntity existing = matchDetailService.getById(matchId);
            if (existing != null && existing.getSyncStatus() == 2) {
                return true;
            }
            // Remove stale record if exists
            if (existing != null) {
                matchDetailService.removeById(matchId);
            }
            return fetchAndSaveDetail(matchId);
        } catch (Exception e) {
            log.error("fetch detail sync for match {} failed: {}", matchId, e.getMessage());
            return false;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
