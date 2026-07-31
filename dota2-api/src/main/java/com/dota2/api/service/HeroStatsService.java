package com.dota2.api.service;

import com.dota2.entity.dao.MatchPlayerDao;
import com.dota2.entity.vo.HeroStatsVo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
@Slf4j
public class HeroStatsService {

    @Autowired
    private MatchPlayerDao matchPlayerDao;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Get hero overview stats with recent 20/50 comparison.
     * All logic is in the SQL query using window functions.
     */
    public List<HeroStatsVo> getHeroStats(String steamId, Integer matchType,
                                           Long startTime, Long endTime,
                                           int minMatches, String sortField, String sortOrder) {
        if (steamId == null || steamId.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // Validate sort params
        String validSort = "games";
        if ("winRate".equals(sortField)) validSort = "winRate";
        String validOrder = "DESC";
        if ("ASC".equalsIgnoreCase(sortOrder)) validOrder = "ASC";

        List<HeroStatsVo> list = matchPlayerDao.queryHeroStats(
                steamId, matchType, startTime, endTime,
                minMatches, validSort, validOrder
        );

        // Compute KDA and recent KDA
        for (HeroStatsVo vo : list) {
            double avgD = vo.getAvgDeaths() != null ? vo.getAvgDeaths() : 1.0;
            vo.setAvgKda(Math.round((vo.getAvgKills() + vo.getAvgAssists()) / Math.max(avgD, 0.1) * 100.0) / 100.0);

            double recentD20 = vo.getRecentDeaths20() != null ? vo.getRecentDeaths20() : 1.0;
            vo.setRecentKda20(Math.round((vo.getRecentKills20() + vo.getRecentAssists20()) / Math.max(recentD20, 0.1) * 100.0) / 100.0);

            double recentD50 = vo.getRecentDeaths50() != null ? vo.getRecentDeaths50() : 1.0;
            vo.setRecentKda50(Math.round((vo.getRecentKills50() + vo.getRecentAssists50()) / Math.max(recentD50, 0.1) * 100.0) / 100.0);
        }

        return list;
    }

    // ========================================================================
    // 全局英雄胜率（从 OpenDota /api/heroStats 获取）
    // ========================================================================
    public List<Map<String, Object>> getGlobalHeroWinRate(Integer days, String gameMode) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String url = "https://api.opendota.com/api/heroStats";
            // Use a short-timeout RestTemplate to fail fast
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(8000);
            RestTemplate fastRt = new RestTemplate(factory);
            String json = fastRt.getForObject(url, String.class);
            // Log first hero's keys to debug field names
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                java.util.Iterator<String> fieldNames = first.fieldNames();
                StringBuilder sb = new StringBuilder();
                while (fieldNames.hasNext()) sb.append(fieldNames.next()).append(",");
                log.info("OpenDota heroStats fields: {}", sb);
            }

            // Pick mode prefix: "turbo" or "pub"
            // NOTE: OpenDota uses SINGULAR field names for pub (pub_pick, pub_win, pub_pick_trend, pub_win_trend)
            // but PLURAL for turbo (turbo_picks, turbo_wins, turbo_picks_trend, turbo_wins_trend)
            boolean isTurbo = "turbo".equals(gameMode);
            String prefix = isTurbo ? "turbo" : "pub";
            String picksField = isTurbo ? prefix + "_picks" : prefix + "_pick";
            String winsField = isTurbo ? prefix + "_wins" : prefix + "_win";
            String picksTrendField = picksField + "_trend";
            String winsTrendField = winsField + "_trend";

            for (JsonNode hero : root) {
                int heroId = hero.path("id").asInt();
                String name = hero.path("localized_name").asText("");

                // Get the trend arrays (7 data points, most recent first or last?)
                JsonNode picksTrend = hero.path(picksTrendField);
                JsonNode winsTrend = hero.path(winsTrendField);

                long totalPicks = 0, totalWins = 0;

                if (picksTrend.isArray() && picksTrend.size() > 0) {
                    int n = picksTrend.size();
                    int takeDays = (days != null && days > 0) ? Math.min(days, n) : n;

                    // Sum the last `takeDays` elements (assumes most recent is last)
                    for (int i = n - takeDays; i < n; i++) {
                        totalPicks += picksTrend.get(i).asLong(0);
                        totalWins += winsTrend.get(i).asLong(0);
                    }
                } else {
                    // Fallback: use lifetime totals
                    totalPicks = hero.path(picksField).asLong(0);
                    totalWins = hero.path(winsField).asLong(0);
                }

                double winRate = totalPicks > 0
                        ? Math.round(10000.0 * totalWins / totalPicks) / 100.0
                        : 0;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("heroId", heroId);
                item.put("heroName", name);
                item.put("games", totalPicks);
                item.put("wins", totalWins);
                item.put("winRate", winRate);
                result.add(item);
            }

            // Sort by games desc
            result.sort((a, b) -> Long.compare(
                    ((Number) b.get("games")).longValue(),
                    ((Number) a.get("games")).longValue()
            ));

        } catch (Exception e) {
            log.warn("Failed to fetch global hero stats from OpenDota: {}", e.getMessage());
        }
        return result;
    }
}
