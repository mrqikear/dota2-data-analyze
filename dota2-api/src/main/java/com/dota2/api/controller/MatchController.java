package com.dota2.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dota2.api.service.MatchSyncService;
import com.dota2.api.service.MatchMvpService;
import com.dota2.api.service.SteamTurboSyncService;
import com.dota2.common.utils.Result;
import com.dota2.entity.dao.MatchPlayerDao;
import com.dota2.entity.entity.MatchDetailEntity;
import com.dota2.entity.entity.MatchMainEntity;
import com.dota2.entity.entity.MatchPlayerEntity;
import com.dota2.entity.form.MatchPageForm;
import com.dota2.entity.service.MatchDetailService;
import com.dota2.entity.service.MatchMainService;
import com.dota2.entity.service.MatchPlayerService;
import com.dota2.entity.entity.SteamAccountEntity;
import com.dota2.entity.service.SteamAccountService;
import com.dota2.entity.vo.MatchPlayerVo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/match")
@Validated
@Slf4j
@Api(tags = "match management")
public class MatchController {

    @Autowired
    private MatchMainService matchMainService;

    @Autowired
    private MatchPlayerService matchPlayerService;

    @Autowired
    private MatchSyncService matchSyncService;

    @Autowired
    private SteamAccountService steamAccountService;

    @Autowired
    private MatchPlayerDao matchPlayerDao;

    @Autowired
    private SteamTurboSyncService steamTurboSyncService;

    @Autowired
    private MatchDetailService matchDetailService;

    @Autowired
    private MatchMvpService matchMvpService;

    @ApiOperation(value = "sync all matches from OpenDota (async)")
    @GetMapping(value = "/sync/{steamId}")
    public Result<String> syncMatches(@PathVariable String steamId,
                                       @RequestParam(required = false) Integer days,
                                       @RequestParam(required = false) String minDate,
                                       @RequestParam(required = false) String maxDate) {
        matchSyncService.syncMatchesAsync(steamId, days, minDate, maxDate);
        return Result.ok("同步任务已启动，正在拉取所有比赛记录...");
    }

    @ApiOperation(value = "sync Turbo matches via Steam Web API")
    @GetMapping(value = "/syncTurbo/{steamId}")
    public Result<String> syncTurbo(@PathVariable String steamId) {
        int count = steamTurboSyncService.syncTurboMatches(steamId);
        return Result.ok("加速模式同步完成，新增 " + count + " 场比赛记录");
    }

    @ApiOperation(value = "sync Turbo matches with date range")
    @GetMapping(value = "/syncTurboDate/{steamId}")
    public Result<String> syncTurboDate(@PathVariable String steamId,
                                         @RequestParam(required = false) Long minTime,
                                         @RequestParam(required = false) Long maxTime) {
        int count = steamTurboSyncService.syncTurboMatches(steamId, minTime, maxTime);
        return Result.ok("加速模式同步完成，新增 " + count + " 场比赛记录");
    }

    @ApiOperation(value = "sync Turbo matches for ALL tracked accounts")
    @GetMapping(value = "/syncTurboAll")
    public Result<String> syncTurboAll() {
        int count = steamTurboSyncService.syncAllAccounts();
        return Result.ok("全账号加速模式同步完成，新增 " + count + " 场比赛记录");
    }

    @ApiOperation(value = "page match player records (join steam_account for nickName, ordered by start_time DESC)")
    @PostMapping(value = "/page")
    public Result<PageInfo<MatchPlayerVo>> page(@RequestBody @Valid MatchPageForm form) {
        String steamId = form.getSteamId();
        int page = form.getPage();
        int size = form.getSize();
        int offset = (page - 1) * size;

        String sortField = form.getSortField();
        String sortOrder = form.getSortOrder();
        if (sortField != null && !sortField.isEmpty()) {
            java.util.Map<String, String> FIELD_MAP = new java.util.HashMap<>();
            FIELD_MAP.put("startTime", "mm.start_time");
            FIELD_MAP.put("createdTime", "mp.created_time");
            String mapped = FIELD_MAP.get(sortField);
            if (mapped != null) {
                sortField = mapped;
            } else {
                sortField = null;
                sortOrder = null;
            }
        }

        Integer gameMode = form.getGameMode();
        Integer heroId = form.getHeroId();
        Boolean parsedFilter = form.getParsed();
        List<MatchPlayerVo> voList = matchPlayerDao.pageWithMain(
                steamId, gameMode, heroId, offset, size,
                sortField, sortOrder,
                parsedFilter
        );
        long total = matchPlayerDao.countWithMain(steamId, gameMode, heroId, parsedFilter);

        // Set parsed flag from match_detail
        if (!voList.isEmpty()) {
            java.util.Set<Long> matchIdSet = voList.stream()
                    .map(MatchPlayerVo::getMatchId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!matchIdSet.isEmpty()) {
                List<MatchDetailEntity> details = matchDetailService.lambdaQuery()
                        .in(MatchDetailEntity::getMatchId, matchIdSet)
                        .select(MatchDetailEntity::getMatchId, MatchDetailEntity::getRawJson)
                        .list();
                java.util.Set<Long> parsedIds = new java.util.HashSet<>();
                for (MatchDetailEntity md : details) {
                    if (md.getRawJson() != null && md.getRawJson().contains("damage_inflictor")) {
                        parsedIds.add(md.getMatchId());
                    }
                }
                for (MatchPlayerVo vo : voList) {
                    vo.setParsed(parsedIds.contains(vo.getMatchId()));
                }
            }
            java.util.Set<String> steamIds = voList.stream()
                    .map(MatchPlayerVo::getSteamId)
                    .collect(java.util.stream.Collectors.toSet());
            final java.util.Map<String, String> nickNameMap;
            if (!steamIds.isEmpty()) {
                nickNameMap = steamAccountService.lambdaQuery()
                        .in(SteamAccountEntity::getSteamId, steamIds)
                        .list().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                SteamAccountEntity::getSteamId,
                                sa -> sa.getNickName() != null ? sa.getNickName() : "",
                                (a, b) -> a
                        ));
            } else {
                nickNameMap = new java.util.HashMap<>();
            }
            for (MatchPlayerVo vo : voList) {
                vo.setNickName(nickNameMap.getOrDefault(vo.getSteamId(), ""));
            }
        }

        com.github.pagehelper.PageInfo<MatchPlayerVo> voPage = new com.github.pagehelper.PageInfo<>();
        voPage.setList(voList);
        voPage.setTotal(total);
        voPage.setPageNum(page);
        voPage.setPageSize(size);
        int pages = (int) ((total + size - 1) / size);
        voPage.setPages(Math.max(pages, 1));
        return Result.ok(voPage);
    }

    @ApiOperation(value = "get player aggregate stats (total, wins, losses, per-hero)")
    @GetMapping(value = "/playerStats/{steamId}")
    public Result<Map<String, Object>> playerStats(@PathVariable String steamId,
                                                    @RequestParam(required = false) Integer gameMode) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();

        // Overall stats
        List<Map<String, Object>> overall = matchPlayerDao.queryPlayerStats(steamId, false, gameMode);
        if (!overall.isEmpty()) {
            Map<String, Object> o = overall.get(0);
            result.put("total", o.get("total"));
            result.put("wins", o.get("wins"));
            result.put("losses", o.get("losses"));
            long t = ((Number) o.get("total")).longValue();
            long w = ((Number) o.get("wins")).longValue();
            result.put("winRate", t > 0 ? Math.round(10000.0 * w / t) / 100.0 : 0);
        }

        // By mode (only show breakdown when no specific mode filter is active)
        List<Map<String, Object>> byMode = matchPlayerDao.queryPlayerStats(steamId, true, gameMode);
        for (Map<String, Object> m : byMode) {
            String mode = (String) m.get("game_mode");
            long t = ((Number) m.get("total")).longValue();
            long w = ((Number) m.get("wins")).longValue();
            m.put("winRate", t > 0 ? Math.round(10000.0 * w / t) / 100.0 : 0);
        }
        result.put("byMode", byMode);

        // Per-hero stats (includes MVP/FMVP)
        List<Map<String, Object>> heroes = matchPlayerDao.queryPlayerHeroStats(steamId, gameMode);
        long totalMvp = 0, totalFmvp = 0;
        for (Map<String, Object> h : heroes) {
            long t = ((Number) h.get("games")).longValue();
            long w = ((Number) h.get("wins")).longValue();
            h.put("winRate", t > 0 ? Math.round(10000.0 * w / t) / 100.0 : 0);
            h.put("avgKda", String.format("%.1f/%.1f/%.1f",
                    ((Number) h.getOrDefault("avg_kills", 0)).doubleValue(),
                    ((Number) h.getOrDefault("avg_deaths", 0)).doubleValue(),
                    ((Number) h.getOrDefault("avg_assists", 0)).doubleValue()));
            totalMvp += ((Number) h.getOrDefault("mvp", 0)).longValue();
            totalFmvp += ((Number) h.getOrDefault("fmvp", 0)).longValue();
        }
        result.put("heroes", heroes);
        result.put("mvp", totalMvp);
        result.put("fmvp", totalFmvp);

        return Result.ok(result);
    }

    @SuppressWarnings("unchecked")
    @ApiOperation(value = "find related matches (teammate/opponent/solo)")
    @PostMapping(value = "/relatedMatches")
    public Result<Map<String, Object>> relatedMatches(@RequestBody Map<String, Object> body) {
        List<String> steamIds = (List<String>) body.get("steamIds");
        String relationType = (String) body.getOrDefault("relationType", "teammate");
        String soloSteamId = (String) body.get("steamId");

        if (steamIds == null || steamIds.size() < 1) {
            return Result.fail("请至少选择 1 个 Steam 账号");
        }
        if ("solo".equals(relationType) && (soloSteamId == null || soloSteamId.isEmpty())) {
            return Result.fail("非关联模式需要指定一个 Steam 账号");
        }

        // 1. Find match IDs based on relation type
        List<Long> matchIds;
        List<String> aggIds = new java.util.ArrayList<>(steamIds);
        if ("opponent".equals(relationType)) {
            String opponentId = (String) body.get("opponentId");
            if (opponentId == null || opponentId.isEmpty()) {
                return Result.fail("对手关联需要输入对手的 Steam ID");
            }
            // Auto-convert account_id (32-bit) to steam_id (64-bit) if needed
            String oppSteamId = opponentId;
            try {
                long val = Long.parseLong(opponentId);
                if (val < 76561197960265728L && val > 0) {
                    oppSteamId = String.valueOf(val + 76561197960265728L);
                }
            } catch (NumberFormatException ignored) {}
            // Step 1: find matches where selected accounts played
            List<Long> candidateMatchIds = matchPlayerDao.findOpponentMatchIds(steamIds, steamIds.size(), oppSteamId);
            // Step 2: also check rawJson for matches where opponent might not have match_player record
            List<Long> allAccountMatchIds = new java.util.ArrayList<>();
            for (String sid : steamIds) {
                allAccountMatchIds.addAll(matchPlayerDao.findMatchIdsBySteamId(sid, 200));
            }
            // Remove dups
            java.util.LinkedHashSet<Long> deduped = new java.util.LinkedHashSet<>(candidateMatchIds);
            ObjectMapper om = new ObjectMapper();
            for (Long mid : allAccountMatchIds) {
                if (deduped.contains(mid)) continue;
                MatchDetailEntity md = matchDetailService.getById(mid);
                if (md != null && md.getRawJson() != null) {
                    try {
                        JsonNode root = om.readTree(md.getRawJson());
                        JsonNode rawPlayers = root.path("players");
                        boolean foundOpponent = false;
                        boolean opponentIsRadiant = false;
                        java.util.Set<String> selectedSet = new java.util.HashSet<>(steamIds);
                        boolean hasSelected = false;
                        boolean selectedIsRadiant = false;
                        if (rawPlayers.isArray()) {
                            for (JsonNode rp : rawPlayers) {
                                Long aid = rp.has("account_id") && !rp.path("account_id").isNull()
                                        ? rp.path("account_id").asLong(0) : null;
                                if (aid == null || aid == 0) continue;
                                String rawSid = String.valueOf(aid + 76561197960265728L);
                                if (rawSid.equals(oppSteamId)) {
                                    foundOpponent = true;
                                    int ps = rp.path("player_slot").asInt(0);
                                    opponentIsRadiant = ps < 128;
                                }
                                if (selectedSet.contains(rawSid)) {
                                    hasSelected = true;
                                    int ps = rp.path("player_slot").asInt(0);
                                    selectedIsRadiant = ps < 128;
                                }
                            }
                        }
                        // Must be on opposite teams
                        if (foundOpponent && hasSelected && opponentIsRadiant != selectedIsRadiant) deduped.add(mid);
                    } catch (Exception e) {
                        log.warn("Failed to parse rawJson for match {}: {}", mid, e.getMessage());
                    }
                }
            }
            matchIds = new java.util.ArrayList<>(deduped);
            matchIds.sort(java.util.Collections.reverseOrder());
            if (matchIds.size() > 50) matchIds = matchIds.subList(0, 50);
        } else if ("solo".equals(relationType)) {
            // Auto-convert account_id to steam_id if needed
            String soloSteamIdFinal = soloSteamId;
            try {
                long val = Long.parseLong(soloSteamId);
                if (val < 76561197960265728L && val > 0) {
                    soloSteamIdFinal = String.valueOf(val + 76561197960265728L);
                }
            } catch (NumberFormatException ignored) {}
            // Exclude ALL tracked accounts in the system except the target one
            List<SteamAccountEntity> allAccounts = steamAccountService.list();
            List<String> excludeIds = new java.util.ArrayList<>();
            for (SteamAccountEntity acc : allAccounts) {
                if (!acc.getSteamId().equals(soloSteamIdFinal)) {
                    excludeIds.add(acc.getSteamId());
                }
            }
            // Mode filter
            Integer soloGameMode = body.get("gameMode") instanceof Number ? ((Number) body.get("gameMode")).intValue() : null;
            // Count total solo matches (DB-level check)
            long soloTotal = matchPlayerDao.countSoloMatchIds(soloSteamIdFinal, excludeIds, soloGameMode);
            // Pagination
            int soloPage = body.get("page") instanceof Number ? ((Number) body.get("page")).intValue() : 1;
            int soloSize = body.get("size") instanceof Number ? ((Number) body.get("size")).intValue() : 20;
            int soloOffset = (soloPage - 1) * soloSize;
            List<Long> soloFromDb = matchPlayerDao.findSoloMatchIds(soloSteamIdFinal, excludeIds, soloOffset, soloSize, soloGameMode);
            // rawJson check for the current page
            List<Long> filteredSolo = new java.util.ArrayList<>();
            for (Long mid : soloFromDb) {
                MatchDetailEntity md = matchDetailService.getById(mid);
                boolean hasOtherTracked = false;
                if (md != null && md.getRawJson() != null) {
                    try {
                        JsonNode root = new ObjectMapper().readTree(md.getRawJson());
                        JsonNode rawPlayers = root.path("players");
                        if (rawPlayers.isArray()) {
                            for (JsonNode rp : rawPlayers) {
                                Long aid = rp.has("account_id") && !rp.path("account_id").isNull()
                                        ? rp.path("account_id").asLong(0) : null;
                                if (aid == null || aid == 0) continue;
                                String rawSid = String.valueOf(aid + 76561197960265728L);
                                if (excludeIds.contains(rawSid)) {
                                    hasOtherTracked = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse rawJson for match {}: {}", mid, e.getMessage());
                    }
                }
                if (!hasOtherTracked) {
                    filteredSolo.add(mid);
                }
            }
            matchIds = filteredSolo;
            aggIds = new java.util.ArrayList<>();
            aggIds.add(soloSteamIdFinal);
            // Store total in response context
            body.put("_soloTotal", soloTotal);
            // Compute solo-specific stats (win/loss + MVP/FMVP from match_main fields)
            Map<String, Object> overallStats = new java.util.LinkedHashMap<>();
            List<Map<String, Object>> soloOverall = matchPlayerDao.querySoloPlayerStats(soloSteamIdFinal, excludeIds, soloGameMode);
            if (!soloOverall.isEmpty()) {
                Map<String, Object> o = soloOverall.get(0);
                overallStats.put("total", o.get("total"));
                overallStats.put("wins", o.get("wins"));
                overallStats.put("losses", o.get("losses"));
                long t = ((Number) o.get("total")).longValue();
                long w = ((Number) o.get("wins")).longValue();
                overallStats.put("winRate", t > 0 ? Math.round(10000.0 * w / t) / 100.0 : 0);
            }
            List<Map<String, Object>> soloMvp = matchPlayerDao.querySoloMvpStats(soloSteamIdFinal, excludeIds, soloGameMode);
            if (!soloMvp.isEmpty()) {
                Map<String, Object> m = soloMvp.get(0);
                overallStats.put("mvp", m.get("mvp"));
                overallStats.put("fmvp", m.get("fmvp"));
            }
            body.put("_soloStats", overallStats);
        } else {
            // teammate (default)
            if (steamIds.size() < 2) return Result.fail("队友关联需要至少选择 2 个账号");
            matchIds = matchPlayerDao.findRelatedMatchIds(steamIds, steamIds.size());
        }

        // 2. Get player details and calculate MVP/FMVP for each match
        Map<String, int[]> mvpCount = new java.util.HashMap<>(); // steamId → [mvpCount, fmvpCount]
        Map<String, Map<Integer, int[]>> heroMvpCount = new java.util.HashMap<>();
        for (String sid : aggIds) {
            mvpCount.put(sid, new int[2]);
            heroMvpCount.put(sid, new java.util.HashMap<>());
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (Long matchId : matchIds) {
            List<MatchPlayerVo> players = matchPlayerDao.listPlayersByMatch(matchId);
            MatchPlayerVo first = players.isEmpty() ? null : players.get(0);

            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("matchId", matchId);
            item.put("startTime", first != null ? first.getStartTime() : 0);
            item.put("duration", first != null ? first.getDuration() : 0);
            item.put("gameMode", first != null ? first.getGameMode() : 0);
            item.put("lobbyType", first != null ? first.getLobbyType() : 0);

            // MVP/FMVP using benchmarks (same algorithm as match detail page)
            String mvpSteamId = null, fmvpSteamId = null;
            double mvpScore = -1, fmvpScore = -1;
            List<Map<String, Object>> playerList = new java.util.ArrayList<>();

            // Load raw_json for benchmark calculation
            MatchDetailEntity detail = matchDetailService.getById(matchId);
            if (detail != null && detail.getRawJson() != null) {
                try {
                    JsonNode root = mapper.readTree(detail.getRawJson());
                    JsonNode rawPlayers = root.path("players");
                    if (rawPlayers.isArray()) {
                        java.util.Map<Long, Double> scores = new java.util.HashMap<>();
                        for (JsonNode rp : rawPlayers) {
                            Long aid = rp.has("account_id") && !rp.path("account_id").isNull()
                                    ? rp.path("account_id").asLong(0) : null;
                            if (aid == null || aid == 0) continue;
                            String pid = String.valueOf(aid + 76561197960265728L);

                            JsonNode benchmarks = rp.path("benchmarks");
                            double avg = 0;
                            if (benchmarks.isObject()) {
                                double totalPct = 0; int count = 0;
                                java.util.Iterator<Map.Entry<String, JsonNode>> fields = benchmarks.fields();
                                while (fields.hasNext()) {
                                    JsonNode v = fields.next().getValue();
                                    if (v.has("pct") && !v.path("pct").isNull()) {
                                        totalPct += v.path("pct").asDouble(0); count++;
                                    }
                                }
                                avg = count > 0 ? totalPct / count : 0;
                            }
                            scores.put(aid, avg);

                            boolean playerWin = rp.path("radiant_win").asBoolean(false)
                                    == (rp.path("player_slot").asInt(0) < 128);
                            if (playerWin && avg > mvpScore) { mvpScore = avg; mvpSteamId = pid; }
                            else if (!playerWin && avg > fmvpScore) { fmvpScore = avg; fmvpSteamId = pid; }
                        }
                        // Merge benchmark scores into playerList
                        for (MatchPlayerVo p : players) {
                            boolean isTracked = steamIds.contains(p.getSteamId());
                            long aid = Long.parseLong(p.getSteamId()) - 76561197960265728L;
                            double score = scores.getOrDefault(aid, 0.0);
                            Map<String, Object> pm = new java.util.LinkedHashMap<>();
                            pm.put("steamId", p.getSteamId());
                            pm.put("nickName", p.getNickName());
                            pm.put("heroId", p.getHeroId());
                            pm.put("kills", p.getKills());
                            pm.put("deaths", p.getDeaths());
                            pm.put("assists", p.getAssists());
                            pm.put("win", p.getWin());
                            pm.put("isTracked", isTracked);
                            pm.put("score", Math.round(score * 1000) / 10.0);
                            playerList.add(pm);
                        }
                    }
                } catch (Exception e) {
                    log.warn("MVP calc failed for match {}: {}", matchId, e.getMessage());
                }
            }

            // Fallback: if no benchmarks, use simple calculation
            if (playerList.isEmpty()) {
                for (MatchPlayerVo p : players) {
                    boolean isTracked = steamIds.contains(p.getSteamId());
                    double score = (p.getKills() + p.getAssists()) * 1.0 * Math.max(p.getGoldPerMin(), 1)
                            / (Math.max(p.getDeaths(), 0) + 1) + p.getHeroDamage() / 100.0;
                    Map<String, Object> pm = new java.util.LinkedHashMap<>();
                    pm.put("steamId", p.getSteamId());
                    pm.put("nickName", p.getNickName());
                    pm.put("heroId", p.getHeroId());
                    pm.put("kills", p.getKills());
                    pm.put("deaths", p.getDeaths());
                    pm.put("assists", p.getAssists());
                    pm.put("win", p.getWin());
                    pm.put("isTracked", isTracked);
                    pm.put("score", Math.round(score * 10) / 10.0);
                    Boolean pw = p.getWin();
                    if (Boolean.TRUE.equals(pw) && score > mvpScore) { mvpScore = score; mvpSteamId = p.getSteamId(); }
                    else if (Boolean.FALSE.equals(pw) && score > fmvpScore) { fmvpScore = score; fmvpSteamId = p.getSteamId(); }
                    playerList.add(pm);
                }
            }

            Map<String, Object> mvpInfo = new java.util.LinkedHashMap<>();
            if (mvpSteamId != null) mvpInfo.put("steamId", mvpSteamId);
            mvpInfo.put("score", mvpScore > 0 ? Math.round(mvpScore * 1000) / 10.0 : 0);
            Map<String, Object> fmvpInfo = new java.util.LinkedHashMap<>();
            if (fmvpSteamId != null) fmvpInfo.put("steamId", fmvpSteamId);
            fmvpInfo.put("score", fmvpScore > 0 ? Math.round(fmvpScore * 1000) / 10.0 : 0);
            item.put("mvp", mvpInfo);
            item.put("fmvp", fmvpInfo);
            item.put("players", playerList);

            // Aggregate counts (per-account + per-hero)
            for (Map<String, Object> pm : playerList) {
                String sid = (String) pm.get("steamId");
                int heroId = ((Number) pm.get("heroId")).intValue();
                if (!heroMvpCount.containsKey(sid)) continue;
                Map<Integer, int[]> heroMap = heroMvpCount.get(sid);
                if (!heroMap.containsKey(heroId)) heroMap.put(heroId, new int[2]);
                if (sid.equals(mvpSteamId)) {
                    mvpCount.get(sid)[0]++;
                    heroMap.get(heroId)[0]++;
                }
                if (sid.equals(fmvpSteamId)) {
                    mvpCount.get(sid)[1]++;
                    heroMap.get(heroId)[1]++;
                }
            }

            result.add(item);
        }

        // Build summary
        List<Map<String, Object>> summary = new java.util.ArrayList<>();
        for (Map.Entry<String, int[]> entry : mvpCount.entrySet()) {
            Map<String, Object> s = new java.util.LinkedHashMap<>();
            s.put("steamId", entry.getKey());
            s.put("mvp", entry.getValue()[0]);
            s.put("fmvp", entry.getValue()[1]);
            s.put("total", entry.getValue()[0] + entry.getValue()[1]);
            // Look up nickName from players
            for (Map<String, Object> match : result) {
                List<Map<String, Object>> pl = (List<Map<String, Object>>) match.get("players");
                if (pl != null) {
                    for (Map<String, Object> pm : pl) {
                        if (entry.getKey().equals(pm.get("steamId")) && pm.get("nickName") != null) {
                            s.put("nickName", pm.get("nickName"));
                            break;
                        }
                    }
                }
                if (s.containsKey("nickName")) break;
            }
            // Per-hero breakdown
            List<Map<String, Object>> heroes = new java.util.ArrayList<>();
            Map<Integer, int[]> heroMap = heroMvpCount.get(entry.getKey());
            if (heroMap != null) {
                for (Map.Entry<Integer, int[]> he : heroMap.entrySet()) {
                    if (he.getValue()[0] == 0 && he.getValue()[1] == 0) continue;
                    Map<String, Object> h = new java.util.LinkedHashMap<>();
                    h.put("heroId", he.getKey());
                    h.put("mvp", he.getValue()[0]);
                    h.put("fmvp", he.getValue()[1]);
                    heroes.add(h);
                }
                heroes.sort((a, b) -> Integer.compare((int) b.get("mvp") + (int) b.get("fmvp"),
                        (int) a.get("mvp") + (int) a.get("fmvp")));
            }
            s.put("heroes", heroes);
            if (!s.containsKey("nickName")) s.put("nickName", "");
            summary.add(s);
        }
        summary.sort((a, b) -> Integer.compare((int) b.get("total"), (int) a.get("total")));

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("summary", summary);
        response.put("matches", result);
        if ("solo".equals(relationType)) {
            response.put("total", body.get("_soloTotal"));
            response.put("page", body.getOrDefault("page", 1));
            response.put("size", body.getOrDefault("size", 20));
            response.put("soloStats", body.get("_soloStats"));
        }
        return Result.ok(response);
    }

    @ApiOperation(value = "backfill MVP/FMVP for all matches with rawJson")
    @PostMapping(value = "/backfillMvp")
    public Result<String> backfillMvp() {
        List<MatchDetailEntity> all = matchDetailService.lambdaQuery()
                .isNotNull(MatchDetailEntity::getRawJson)
                .list();
        int success = 0, fail = 0;
        for (MatchDetailEntity md : all) {
            try {
                boolean ok = matchMvpService.calculateAndSave(md.getMatchId());
                if (ok) success++; else fail++;
            } catch (Exception e) {
                fail++;
            }
            if ((success + fail) % 100 == 0) {
                log.info("[Backfill] progress: {}/{}", success + fail, all.size());
            }
        }
        return Result.ok("回填完成: 成功 " + success + " 条, 失败 " + fail + " 条");
    }
}
