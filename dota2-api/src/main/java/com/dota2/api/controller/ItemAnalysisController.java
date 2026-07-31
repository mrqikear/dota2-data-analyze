package com.dota2.api.controller;

import com.dota2.api.service.ItemAnalysisService;
import com.dota2.common.utils.Result;
import com.dota2.entity.form.ItemAnalysisForm;
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
@RequestMapping(value = "/itemAnalysis")
@Validated
@Slf4j
@Api(tags = "item analysis")
public class ItemAnalysisController {

    @Autowired
    private ItemAnalysisService itemAnalysisService;

    @ApiOperation(value = "4.1 starting item combos")
    @PostMapping("/startingItems")
    public Result<List<Map<String, Object>>> startingItems(@RequestBody @Valid ItemAnalysisForm form) {
        return Result.ok(itemAnalysisService.getStartingItems(form.getSteamId()));
    }

    @ApiOperation(value = "4.2 item build routes (top N big items per match)")
    @PostMapping("/buildRoutes")
    public Result<List<Map<String, Object>>> buildRoutes(@RequestBody @Valid ItemAnalysisForm form,
                                                          @RequestParam(defaultValue = "5") Integer topN) {
        return Result.ok(itemAnalysisService.getBuildRoutes(form.getSteamId(), form.getHeroId(), topN));
    }

    @ApiOperation(value = "4.3 single item stats (usage, win rate)")
    @PostMapping("/itemStats")
    public Result<List<Map<String, Object>>> itemStats(@RequestBody @Valid ItemAnalysisForm form) {
        return Result.ok(itemAnalysisService.getItemStats(form.getSteamId(), form.getHeroId()));
    }

    @ApiOperation(value = "4.4 item win rate contribution (delta with vs without)")
    @PostMapping("/winContribution")
    public Result<List<Map<String, Object>>> winContribution(@RequestBody @Valid ItemAnalysisForm form) {
        return Result.ok(itemAnalysisService.getWinContribution(form.getSteamId(), form.getHeroId()));
    }

    @ApiOperation(value = "4.5 personal vs global item comparison")
    @PostMapping("/compareGlobal")
    public Result<Map<String, Object>> compareGlobal(@RequestBody @Valid ItemAnalysisForm form) {
        return Result.ok(itemAnalysisService.getCompareGlobal(form.getSteamId(), form.getHeroId()));
    }

    @ApiOperation(value = "4.6 item build archetype classification")
    @PostMapping("/archetype")
    public Result<List<Map<String, Object>>> archetype(@RequestBody @Valid ItemAnalysisForm form) {
        return Result.ok(itemAnalysisService.getArchetypeStats(form.getSteamId(), form.getHeroId()));
    }
}
