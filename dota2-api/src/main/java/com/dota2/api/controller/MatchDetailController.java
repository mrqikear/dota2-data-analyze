package com.dota2.api.controller;

import com.dota2.api.service.MatchDetailSyncService;
import com.dota2.common.utils.Result;
import com.dota2.entity.dao.MatchPlayerDao;
import com.dota2.entity.entity.MatchDetailEntity;
import com.dota2.entity.entity.MatchMainEntity;
import com.dota2.entity.service.MatchDetailService;
import com.dota2.entity.service.MatchMainService;
import com.dota2.entity.vo.MatchPlayerVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/match/detail")
@Validated
@Slf4j
@Api(tags = "match detail")
public class MatchDetailController {

    @Autowired
    private MatchDetailService matchDetailService;

    @Autowired
    private MatchDetailSyncService matchDetailSyncService;

    @Autowired
    private MatchPlayerDao matchPlayerDao;

    @Autowired
    private MatchMainService matchMainService;

    @ApiOperation(value = "get match detail by matchId")
    @GetMapping(value = "/{matchId}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long matchId) {
        MatchDetailEntity detail = matchDetailService.getById(matchId);
        if (detail == null) {
            return Result.ok(null);
        }
        List<MatchPlayerVo> players = matchPlayerDao.listPlayersByMatch(matchId);
        Map<String, Object> result = new HashMap<>();
        result.put("detail", detail);
        result.put("players", players);

        // Add match_main start_time
        MatchMainEntity main = matchMainService.getById(matchId);
        result.put("startTime", main != null ? main.getStartTime() : 0);

        // MVP / FMVP based on benchmark percentiles
        if (detail.getRawJson() != null && players.size() > 0) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(detail.getRawJson());
                JsonNode rawPlayers = root.path("players");
                if (rawPlayers.isArray()) {
                    String mvpSteamId = null;
                    String fmvpSteamId = null;
                    double mvpScore = -1;
                    double fmvpScore = -1;

                    for (JsonNode p : rawPlayers) {
                        Long accountId = p.has("account_id") && !p.path("account_id").isNull()
                                ? p.path("account_id").asLong(0) : null;
                        if (accountId == null || accountId == 0) continue;
                        String pid = String.valueOf(accountId + 76561197960265728L);
                        boolean radiantWin = root.path("radiant_win").asBoolean(false);
                        boolean isRadiant = p.path("player_slot").asInt(0) < 128;
                        boolean playerWin = isRadiant == radiantWin;

                        // Calculate average benchmark percentile
                        JsonNode benchmarks = p.path("benchmarks");
                        if (!benchmarks.isObject()) continue;
                        double totalPct = 0;
                        int count = 0;
                        java.util.Iterator<Map.Entry<String, JsonNode>> fields = benchmarks.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            JsonNode val = field.getValue();
                            if (val.has("pct") && !val.path("pct").isNull()) {
                                totalPct += val.path("pct").asDouble(0);
                                count++;
                            }
                        }
                        double avg = count > 0 ? totalPct / count : 0;

                        if (playerWin && avg > mvpScore) {
                            mvpScore = avg;
                            mvpSteamId = pid;
                        } else if (!playerWin && avg > fmvpScore) {
                            fmvpScore = avg;
                            fmvpSteamId = pid;
                        }
                    }

                    Map<String, Object> mvpInfo = new HashMap<>();
                    if (mvpSteamId != null) {
                        mvpInfo.put("steamId", mvpSteamId);
                        mvpInfo.put("score", Math.round(mvpScore * 1000) / 10.0);
                    }
                    Map<String, Object> fmvpInfo = new HashMap<>();
                    if (fmvpSteamId != null) {
                        fmvpInfo.put("steamId", fmvpSteamId);
                        fmvpInfo.put("score", Math.round(fmvpScore * 1000) / 10.0);
                    }
                    result.put("mvp", mvpInfo);
                    result.put("fmvp", fmvpInfo);
                }
            } catch (Exception e) {
                log.warn("MVP calculation failed for match {}: {}", matchId, e.getMessage());
            }
        }

        return Result.ok(result);
    }

    @ApiOperation(value = "fetch and save match detail from OpenDota")
    @PostMapping(value = "/fetch/{matchId}")
    public Result<String> fetchDetail(@PathVariable Long matchId) {
        boolean ok = matchDetailSyncService.fetchAndSaveDetailSync(matchId);
        if (ok) {
            return Result.ok("详细数据同步成功");
        }
        return Result.fail("同步失败，请检查日志");
    }

    @ApiOperation(value = "get damage breakdown for a match (lightweight)")
    @GetMapping(value = "/damage/{matchId}")
    public Result<java.util.Map<String, Object>> getDamage(@PathVariable Long matchId) {
        MatchDetailEntity detail = matchDetailService.getById(matchId);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("parsed", false);
        if (detail != null && detail.getRawJson() != null && detail.getRawJson().contains("damage_inflictor")) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(detail.getRawJson());
                com.fasterxml.jackson.databind.JsonNode players = root.path("players");
                java.util.List<java.util.Map<String, Object>> playerDamages = new java.util.ArrayList<>();
                if (players.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode p : players) {
                        Long accountId = p.has("account_id") && !p.path("account_id").isNull()
                                ? p.path("account_id").asLong(0) : null;
                        if (accountId == null || accountId == 0) continue;
                        String steamId = String.valueOf(accountId + 76561197960265728L);
                        java.util.Map<String, Object> pd = new java.util.LinkedHashMap<>();
                        pd.put("steamId", steamId);
                        pd.put("heroId", p.path("hero_id").asInt(0));
                        pd.put("heroDamage", p.path("hero_damage").asInt(0));
                        pd.put("damageTaken", p.path("damage_taken").asInt(0));
                        // Damage inflicted
                        com.fasterxml.jackson.databind.JsonNode di = p.path("damage_inflictor");
                        if (di.isObject()) {
                            java.util.Map<String, Object> inflictor = new java.util.LinkedHashMap<>();
                            java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = di.fields();
                            while (fields.hasNext()) {
                                java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> f = fields.next();
                                inflictor.put(f.getKey(), f.getValue().asLong(0));
                            }
                            pd.put("damageInflictor", inflictor);
                        }
                        // Damage received
                        com.fasterxml.jackson.databind.JsonNode dir = p.path("damage_inflictor_received");
                        if (dir.isObject()) {
                            java.util.Map<String, Object> received = new java.util.LinkedHashMap<>();
                            java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = dir.fields();
                            while (fields.hasNext()) {
                                java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> f = fields.next();
                                received.put(f.getKey(), f.getValue().asLong(0));
                            }
                            pd.put("damageInflictorReceived", received);
                        }
                        playerDamages.add(pd);
                    }
                }
                result.put("players", playerDamages);
                result.put("parsed", true);
            } catch (Exception e) {
                log.warn("Failed to parse damage for match {}: {}", matchId, e.getMessage());
            }
        }
        return Result.ok(result);
    }

    @ApiOperation(value = "request OpenDota to fully parse the replay")
    @PostMapping(value = "/request/{matchId}")
    public Result<String> requestParse(@PathVariable Long matchId) {
        try {
            String url = "https://api.opendota.com/api/request/{matchId}";
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            rt.getForEntity(url, String.class, matchId);
            return Result.ok("已请求 OpenDota 解析，请等待几分钟后刷新");
        } catch (Exception e) {
            return Result.fail("请求失败: " + e.getMessage());
        }
    }
}
