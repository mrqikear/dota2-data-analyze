package com.dota2.api.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dota2.entity.entity.MatchDetailEntity;
import com.dota2.entity.entity.MatchMainEntity;
import com.dota2.entity.service.MatchDetailService;
import com.dota2.entity.service.MatchMainService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to calculate and persist MVP/FMVP for each match.
 * MVP = winning team's best player (highest avg benchmark percentile, or KDA formula fallback)
 * FMVP = losing team's best player
 */
@Slf4j
@Service
public class MatchMvpService {

    private static final long STEAM64_OFFSET = 76561197960265728L;

    @Autowired
    private MatchDetailService matchDetailService;

    @Autowired
    private MatchMainService matchMainService;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Calculate MVP/FMVP for a match and persist to match_main.
     * Returns true if successfully calculated.
     */
    public boolean calculateAndSave(Long matchId) {
        MatchDetailEntity detail = matchDetailService.getById(matchId);
        if (detail == null || detail.getRawJson() == null) {
            return false;
        }

        String mvpSteamId = null;
        String fmvpSteamId = null;

        try {
            JsonNode root = mapper.readTree(detail.getRawJson());
            boolean radiantWin = root.path("radiant_win").asBoolean(false);
            JsonNode rawPlayers = root.path("players");
            if (!rawPlayers.isArray() || rawPlayers.size() == 0) {
                return false;
            }

            double mvpScore = -1, fmvpScore = -1;

            for (JsonNode rp : rawPlayers) {
                Long aid = rp.has("account_id") && !rp.path("account_id").isNull()
                        ? rp.path("account_id").asLong(0) : null;
                if (aid == null || aid == 0) continue;

                String pid = String.valueOf(aid + STEAM64_OFFSET);
                int playerSlot = rp.path("player_slot").asInt(0);
                boolean isRadiant = playerSlot < 128;
                boolean playerWin = isRadiant == radiantWin;

                double score = 0;

                // Try benchmarks first
                JsonNode benchmarks = rp.path("benchmarks");
                if (benchmarks.isObject()) {
                    double totalPct = 0;
                    int count = 0;
                    java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = benchmarks.fields();
                    while (fields.hasNext()) {
                        JsonNode v = fields.next().getValue();
                        if (v.has("pct") && !v.path("pct").isNull()) {
                            totalPct += v.path("pct").asDouble(0);
                            count++;
                        }
                    }
                    if (count > 0) {
                        score = totalPct / count;
                    }
                }

                // Fallback: KDA formula
                if (score == 0) {
                    int kills = rp.path("kills").asInt(0);
                    int deaths = rp.path("deaths").asInt(0);
                    int assists = rp.path("assists").asInt(0);
                    int gpm = rp.path("gold_per_min").asInt(0);
                    int damage = rp.path("hero_damage").asInt(0);
                    score = (kills + assists) * 1.0 * Math.max(gpm, 1) / (Math.max(deaths, 0) + 1) + damage / 100.0;
                }

                if (playerWin && score > mvpScore) {
                    mvpScore = score;
                    mvpSteamId = pid;
                } else if (!playerWin && score > fmvpScore) {
                    fmvpScore = score;
                    fmvpSteamId = pid;
                }
            }

            // Update match_main
            matchMainService.lambdaUpdate()
                    .eq(MatchMainEntity::getMatchId, matchId)
                    .set(MatchMainEntity::getMvpSteamId, mvpSteamId)
                    .set(MatchMainEntity::getFmvpSteamId, fmvpSteamId)
                    .update();

            log.debug("[MatchMvp] match_id={} mvp={} fmvp={}", matchId, mvpSteamId, fmvpSteamId);
            return true;

        } catch (Exception e) {
            log.warn("[MatchMvp] failed for match {}: {}", matchId, e.getMessage());
            return false;
        }
    }
}
