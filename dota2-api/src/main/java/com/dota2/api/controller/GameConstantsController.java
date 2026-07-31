package com.dota2.api.controller;

import com.dota2.api.service.GameConstantsService;
import com.dota2.common.utils.Result;
import com.dota2.entity.entity.GameConstantsEntity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/constants")
@Validated
@Slf4j
@Api(tags = "game constants cache")
public class GameConstantsController {

    @Autowired
    private GameConstantsService gameConstantsService;

    @ApiOperation(value = "trigger manual sync of all game constants")
    @PostMapping(value = "/sync")
    public Result<String> sync() {
        gameConstantsService.syncAll();
        return Result.ok("常量同步完成");
    }

    @ApiOperation(value = "get cached constants by type (abilities/heroes/items/game_mode/lobby_type)")
    @GetMapping(value = "/{type}")
    public Result<String> getConstants(@PathVariable String type) {
        GameConstantsEntity entity = gameConstantsService.getCached(type);
        if (entity == null) return Result.fail(type + " 尚未缓存");
        return Result.ok(entity.getDataJson());
    }
}
