package com.dota2.api.controller;

import com.dota2.api.service.HeroStatsService;
import com.dota2.api.service.HeroDailyStatsService;
import com.dota2.common.utils.Result;
import com.dota2.entity.form.HeroStatsForm;
import com.dota2.entity.vo.HeroStatsVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/analysis")
@Validated
@Slf4j
@Api(tags = "analysis - hero stats")
public class HeroStatsController {

    @Autowired
    private HeroStatsService heroStatsService;

    @Autowired
    private HeroDailyStatsService heroDailyStatsService;

    @ApiOperation(value = "get hero overview stats grouped by hero + account")
    @PostMapping(value = "/heroStats")
    public Result<List<HeroStatsVo>> heroStats(@RequestBody @Valid HeroStatsForm form) {
        List<HeroStatsVo> list = heroStatsService.getHeroStats(
                form.getSteamId(),
                form.getMatchType(),
                form.getStartTime(),
                form.getEndTime(),
                form.getMinMatches() != null ? form.getMinMatches() : 1,
                form.getSortField() != null ? form.getSortField() : "games",
                form.getSortOrder() != null ? form.getSortOrder() : "DESC"
        );
        return Result.ok(list);
    }

    @ApiOperation(value = "get ALL heroes global win rate (cached daily, or live from OpenDota)")
    @GetMapping(value = "/allHeroWinRate")
    public Result<Map<String, Object>> allHeroWinRate(@RequestParam(required = false) Integer days) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();

        // 有天数筛选时实时调 OpenDota，无筛选时走缓存（无缓存则自动拉取）
        if (days != null && days > 0) {
            List<Map<String, Object>> pub = heroStatsService.getGlobalHeroWinRate(days, "pub");
            List<Map<String, Object>> turbo = heroStatsService.getGlobalHeroWinRate(days, "turbo");
            result.put("pub", pub);
            result.put("turbo", turbo);
        } else {
            // getOrFetch: 有缓存读缓存，无缓存自动拉取后读缓存
            List<com.dota2.entity.entity.HeroDailyStatsEntity> list = heroDailyStatsService.getOrFetch();
            for (com.dota2.entity.entity.HeroDailyStatsEntity e : list) {
                Map<String, Object> pubItem = new java.util.LinkedHashMap<>();
                pubItem.put("heroId", e.getHeroId());
                pubItem.put("heroName", e.getHeroName());
                pubItem.put("games", e.getPubPicks());
                pubItem.put("wins", e.getPubWins());
                pubItem.put("winRate", e.getPubPicks() > 0
                        ? Math.round(10000.0 * e.getPubWins() / e.getPubPicks()) / 100.0 : 0);
                ((List<Map<String, Object>>) result.computeIfAbsent("pub", k -> new java.util.ArrayList<>())).add(pubItem);
                Map<String, Object> turboItem = new java.util.LinkedHashMap<>();
                turboItem.put("heroId", e.getHeroId());
                turboItem.put("heroName", e.getHeroName());
                turboItem.put("games", e.getTurboPicks());
                turboItem.put("wins", e.getTurboWins());
                turboItem.put("winRate", e.getTurboPicks() > 0
                        ? Math.round(10000.0 * e.getTurboWins() / e.getTurboPicks()) / 100.0 : 0);
                ((List<Map<String, Object>>) result.computeIfAbsent("turbo", k -> new java.util.ArrayList<>())).add(turboItem);
            }
            // sort by games desc
            ((List<Map<String, Object>>) result.get("pub")).sort((a, b) -> Long.compare(
                    ((Number) b.get("games")).longValue(), ((Number) a.get("games")).longValue()));
            ((List<Map<String, Object>>) result.get("turbo")).sort((a, b) -> Long.compare(
                    ((Number) b.get("games")).longValue(), ((Number) a.get("games")).longValue()));
        }

        return Result.ok(result);
    }

    @ApiOperation(value = "manually trigger daily hero stats cache refresh")
    @PostMapping(value = "/refreshHeroCache")
    public Result<String> refreshHeroCache() {
        heroDailyStatsService.fetchAndSave();
        return Result.ok("英雄数据缓存刷新完成");
    }
}
