package com.dota2.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dota2.entity.entity.HeroDailyStatsEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dota2.entity.dao.HeroDailyStatsDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HeroDailyStatsService extends ServiceImpl<HeroDailyStatsDao, HeroDailyStatsEntity> {

    private static final String OPENDOTA_HEROSTATS_URL = "https://api.opendota.com/api/heroStats";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取今日缓存，无缓存时自动拉取 OpenDota
     */
    public List<HeroDailyStatsEntity> getOrFetch() {
        List<HeroDailyStatsEntity> cached = getTodayStats();
        if (!cached.isEmpty()) return cached;
        log.info("[HeroDailyStats] cache miss, fetching from OpenDota...");
        fetchAndSave();
        return getTodayStats();
    }

    /**
     * 手动触发拉取
     */
    public void fetchAndSave() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        RestTemplate rt = new RestTemplate(factory);

        String json = rt.getForObject(OPENDOTA_HEROSTATS_URL, String.class);
        ObjectMapper mapper = new ObjectMapper();

        LocalDate today = LocalDate.now();
        // 删除今天已有的记录（重新拉取）
        remove(new LambdaQueryWrapper<HeroDailyStatsEntity>()
                .eq(HeroDailyStatsEntity::getFetchDate, today));

        try {
            JsonNode root = mapper.readTree(json);
            List<HeroDailyStatsEntity> list = new ArrayList<>();
            for (JsonNode hero : root) {
                HeroDailyStatsEntity e = new HeroDailyStatsEntity();
                e.setHeroId(hero.path("id").asInt());
                e.setHeroName(hero.path("localized_name").asText(""));
                e.setPubPicks(hero.path("pub_pick").asLong(0));
                e.setPubWins(hero.path("pub_win").asLong(0));
                e.setTurboPicks(hero.path("turbo_picks").asLong(0));
                e.setTurboWins(hero.path("turbo_wins").asLong(0));
                e.setPubPicksTrend(toJsonString(hero.path("pub_pick_trend")));
                e.setPubWinsTrend(toJsonString(hero.path("pub_win_trend")));
                e.setTurboPicksTrend(toJsonString(hero.path("turbo_picks_trend")));
                e.setTurboWinsTrend(toJsonString(hero.path("turbo_wins_trend")));
                e.setFetchDate(today);
                e.setCreatedTime(LocalDateTime.now());
                list.add(e);
            }
            saveBatch(list, 200);
            log.info("[HeroDailyStats] saved {} heroes for {}", list.size(), today);
        } catch (Exception e) {
            log.warn("[HeroDailyStats] parse failed: {}", e.getMessage());
        }
    }

    private String toJsonString(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) return "[]";
        try {
            return new ObjectMapper().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 从缓存读取今日数据
     */
    public List<HeroDailyStatsEntity> getTodayStats() {
        return list(new LambdaQueryWrapper<HeroDailyStatsEntity>()
                .eq(HeroDailyStatsEntity::getFetchDate, LocalDate.now())
                .orderByAsc(HeroDailyStatsEntity::getHeroId));
    }
}
