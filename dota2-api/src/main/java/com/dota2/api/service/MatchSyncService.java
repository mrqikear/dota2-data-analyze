package com.dota2.api.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dota2.entity.entity.MatchMainEntity;
import com.dota2.entity.entity.MatchPlayerEntity;
import com.dota2.entity.entity.SteamAccountEntity;
import com.dota2.entity.service.MatchMainService;
import com.dota2.entity.service.MatchPlayerService;
import com.dota2.entity.service.SteamAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchSyncService {

    private static final long STEAM64_OFFSET = 76561197960265728L;

    private static final String OPENDOTA_MATCHES_URL =
            "https://api.opendota.com/api/players/{accountId}/matches?limit=100";

    private static final String OPENDOTA_RECENT_URL =
            "https://api.opendota.com/api/players/{accountId}/recentMatches";

    private static final String OPENDOTA_REQUEST_URL =
            "https://api.opendota.com/api/request/{matchId}";

    private static final int MAX_TOTAL_MATCHES = 5000;

    private static final int GAME_MODE_TURBO = 23;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MatchMainService matchMainService;

    @Autowired
    private MatchPlayerService matchPlayerService;

    @Autowired
    private SteamAccountService steamAccountService;

    private long toAccountId(String steamId) {
        return Long.parseLong(steamId) - STEAM64_OFFSET;
    }

    /**
     * Scheduled sync: run syncMatches for ALL tracked accounts every 6 hours.
     */
    @Scheduled(initialDelay = 60000, fixedRate = 21600000) // 6 hours
    public void scheduledSyncAll() {
        log.info("[MatchSync] ===== scheduled 6h sync start =====");
        List<SteamAccountEntity> accounts = steamAccountService.list();
        for (SteamAccountEntity a : accounts) {
            try {
                int count = syncMatches(a.getSteamId());
                log.info("[MatchSync] account {} synced: {} new matches", a.getNickName(), count);
            } catch (Exception e) {
                log.warn("[MatchSync] account {} sync failed: {}", a.getNickName(), e.getMessage());
            }
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }
        log.info("[MatchSync] ===== scheduled 6h sync done =====");
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Integer> syncMatchesAsync(String steamId, Integer days,
                                                        String minDate, String maxDate) {
        return CompletableFuture.supplyAsync(() -> syncMatches(steamId, days, minDate, maxDate));
    }

    @SuppressWarnings("unchecked")
    public int syncMatches(String steamId) {
        return syncMatches(steamId, null, null, null);
    }

    @SuppressWarnings("unchecked")
    public int syncMatches(String steamId, Integer days, String minDate, String maxDate) {
        long accountId = toAccountId(steamId);
        log.info("syncMatches: steamId={} accountId={} days={} minDate={} maxDate={}",
                steamId, accountId, days, minDate, maxDate);

        // 1. Trigger OpenDota tracking/refresh
        try {
            restTemplate.postForEntity(
                    "https://api.opendota.com/api/players/{accountId}/refresh",
                    null, String.class, accountId);
            log.info("triggered refresh for accountId={}", accountId);
        } catch (Exception e) {
            log.warn("refresh failed for accountId={}: {}", accountId, e.getMessage());
        }

        List<Map<String, Object>> allRaw = new ArrayList<>();

        // 2. Fetch recent matches (includes unparsed Turbo, up to 20)
        try {
            ResponseEntity<List> recentResp = restTemplate.getForEntity(
                    OPENDOTA_RECENT_URL, List.class, accountId);
            if (recentResp.getStatusCode().is2xxSuccessful() && recentResp.getBody() != null) {
                List<Map<String, Object>> recentBatch = recentResp.getBody();
                if (!recentBatch.isEmpty()) {
                    log.info("steamId={} recentMatches={}", steamId, recentBatch.size());
                    allRaw.addAll(recentBatch);
                }
            }
        } catch (Exception e) {
            log.warn("recentMatches failed for accountId={}: {}", accountId, e.getMessage());
        }

        // 3. Fetch all parsed matches via paginated endpoint
        //    Note: Turbo (game_mode=23) is NOT indexed by OpenDota in the /matches endpoint.
        //    Turbo matches are only discoverable via /recentMatches (~20 most recent).
        //    We request parsing below so they may appear here on future syncs.
        StringBuilder urlBuilder = new StringBuilder(OPENDOTA_MATCHES_URL);
        if (minDate != null && !minDate.isEmpty()) urlBuilder.append("&min_date=").append(minDate);
        if (maxDate != null && !maxDate.isEmpty()) urlBuilder.append("&max_date=").append(maxDate);
        String matchesUrl = urlBuilder.toString();
        fetchPaginated(accountId, matchesUrl, allRaw);

        // Deduplicate by match_id (recentMatches may overlap with paginated results)
        allRaw = allRaw.stream()
                .collect(Collectors.toMap(
                        m -> m.get("match_id"),
                        m -> m,
                        (a, b) -> a))
                .values().stream()
                .collect(Collectors.toList());

        log.info("steamId={} total raw matches fetched={}", steamId, allRaw.size());
        if (allRaw.isEmpty()) {
            log.info("steamId={} no matches from OpenDota (tracking just requested, try again later)", steamId);
            return -1;
        }

        // Existing match_ids for this steam_id (already in match_player)
        Set<Long> existingMatchIds = matchPlayerService.lambdaQuery()
                .eq(MatchPlayerEntity::getSteamId, steamId)
                .list().stream()
                .map(MatchPlayerEntity::getMatchId)
                .collect(Collectors.toSet());

        List<MatchPlayerEntity> playerBatch = new ArrayList<>();
        int newCount = 0;

        for (Map<String, Object> raw : allRaw) {
            Long matchId = toLong(raw.get("match_id"));
            if (matchId == null || existingMatchIds.contains(matchId)) continue;

            // Upsert match_main
            MatchMainEntity mm = matchMainService.getById(matchId);
            if (mm == null) {
                mm = new MatchMainEntity();
                mm.setMatchId(matchId);
                mm.setStartTime(toLong(raw.get("start_time")));
                mm.setDuration(toInt(raw.get("duration")));
                mm.setGameMode(toInt(raw.get("game_mode")));
                mm.setLobbyType(toInt(raw.get("lobby_type")));
                mm.setCreatedTime(LocalDateTime.now());
                matchMainService.save(mm);
            } else if (mm.getStartTime() == null || mm.getStartTime() == 0) {
                // Update skeleton match with real data
                mm.setStartTime(toLong(raw.get("start_time")));
                mm.setDuration(toInt(raw.get("duration")));
                mm.setGameMode(toInt(raw.get("game_mode")));
                mm.setLobbyType(toInt(raw.get("lobby_type")));
                matchMainService.updateById(mm);
            }

            // Insert match_player
            MatchPlayerEntity mp = new MatchPlayerEntity();
            mp.setMatchId(matchId);
            mp.setSteamId(steamId);
            mp.setHeroId(toInt(raw.get("hero_id")));
            mp.setKills(toInt(raw.get("kills")));
            mp.setDeaths(toInt(raw.get("deaths")));
            mp.setAssists(toInt(raw.get("assists")));

            Boolean radiantWin = raw.get("radiant_win") != null
                    && (Boolean) raw.get("radiant_win");
            Integer playerSlot = toInt(raw.get("player_slot"));
            boolean isRadiant = playerSlot != null && playerSlot < 128;
            mp.setWin(isRadiant == radiantWin);

            mp.setGoldPerMin(toInt(raw.get("gold_per_min")));
            mp.setXpPerMin(toInt(raw.get("xp_per_min")));
            mp.setLastHits(toInt(raw.get("last_hits")));
            mp.setDenies(toInt(raw.get("denies")));
            mp.setHeroDamage(toInt(raw.get("hero_damage")));
            mp.setTowerDamage(toInt(raw.get("tower_damage")));
            mp.setHeroHealing(toInt(raw.get("hero_healing")));
            mp.setCreatedTime(LocalDateTime.now());

            playerBatch.add(mp);
            existingMatchIds.add(matchId);
            newCount++;
        }

        if (!playerBatch.isEmpty()) {
            matchPlayerService.saveBatch(playerBatch, 100);
        }

        // 4. Request parsing for newly discovered Turbo matches
        //    RecentMatches already provides complete stats, but requesting
        //    parsing may help OpenDota index these matches for future queries.
        requestTurboParsing(steamId, allRaw);

        // Sync player profile from OpenDota (nickname, avatar)
        try {
            String profileUrl = "https://api.opendota.com/api/players/{accountId}";
            ResponseEntity<Map> profileResp = restTemplate.getForEntity(profileUrl, Map.class, accountId);
            if (profileResp.getStatusCode().is2xxSuccessful() && profileResp.getBody() != null) {
                Map<String, Object> profile = (Map<String, Object>) profileResp.getBody().get("profile");
                if (profile != null) {
                    String nickName = profile.get("personaname") != null ? profile.get("personaname").toString() : null;
                    String avatar = profile.get("avatarfull") != null ? profile.get("avatarfull").toString() : null;
                    String profileUrlStr = profile.get("profileurl") != null ? profile.get("profileurl").toString() : null;

                    SteamAccountEntity updateEntity = new SteamAccountEntity();
                    if (nickName != null) updateEntity.setNickName(nickName);
                    if (avatar != null) updateEntity.setAvatar(avatar);
                    if (profileUrlStr != null) updateEntity.setProfileUrl(profileUrlStr);

                    steamAccountService.update(updateEntity,
                            Wrappers.<SteamAccountEntity>lambdaUpdate()
                                    .eq(SteamAccountEntity::getSteamId, steamId));
                    log.info("synced profile for steamId={} nickName={}", steamId, nickName);
                }
            }
        } catch (Exception e) {
            log.warn("profile sync failed for steamId={}: {}", steamId, e.getMessage());
        }

        // Update last fetch time
        steamAccountService.update(
                Wrappers.<SteamAccountEntity>lambdaUpdate()
                        .eq(SteamAccountEntity::getSteamId, steamId)
                        .setSql("last_fetch_time = NOW()")
        );

        log.info("steamId={} done, new player records={}", steamId, newCount);
        return newCount;
    }

    /**
     * Request parsing for newly discovered Turbo matches (game_mode=23).
     * OpenDota doesn't index Turbo in /matches endpoint by default, but
     * requesting parsing individually may help them appear on future syncs.
     * The endpoint is idempotent - duplicate requests are harmless.
     */
    private void requestTurboParsing(String steamId, List<Map<String, Object>> allRaw) {
        // Collect Turbo match IDs from the newly fetched raw data
        Set<Long> turboIds = allRaw.stream()
                .filter(m -> {
                    Object gm = m.get("game_mode");
                    return gm != null && toInt(gm) == GAME_MODE_TURBO;
                })
                .map(m -> toLong(m.get("match_id")))
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (turboIds.isEmpty()) {
            log.info("steamId={} no Turbo matches to request parse", steamId);
            return;
        }

        log.info("steamId={} requesting parse for {} Turbo matches", steamId, turboIds.size());
        for (Long matchId : turboIds) {
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(
                        OPENDOTA_REQUEST_URL, null, String.class, matchId);
                log.debug("requested parse for Turbo match {}: {}", matchId, resp.getStatusCode());
                Thread.sleep(500);
            } catch (Exception e) {
                log.warn("request parse for Turbo match {} failed: {}", matchId, e.getMessage());
            }
        }
    }

    /** Paginated fetch from OpenDota with rate limiting */
    @SuppressWarnings("unchecked")
    private void fetchPaginated(long accountId, String baseUrl, List<Map<String, Object>> target) {
        int offset = 0;
        while (offset < MAX_TOTAL_MATCHES) {
            String url = baseUrl + "&offset=" + offset;
            try {
                ResponseEntity<List> resp = restTemplate.getForEntity(url, List.class, accountId);
                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    log.warn("OpenDota API error at offset={}: {}", offset, resp.getStatusCode());
                    break;
                }
                List<Map<String, Object>> batch = resp.getBody();
                if (batch.isEmpty()) break;
                target.addAll(batch);
                offset += 100;
                try { Thread.sleep(1100); } catch (InterruptedException ignored) {}
            } catch (Exception e) {
                log.warn("OpenDota fetch failed at offset={}: {}", offset, e.getMessage());
                break;
            }
        }
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return 0; }
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }
}
