package com.dota2.api.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.dota2.api.dto.SteamPlayerDto;
import com.dota2.api.service.SteamSyncService;
import com.dota2.common.annotation.SYSLOG;
import com.dota2.common.exception.Dota2Exception;
import com.dota2.common.utils.OperationLogHelper;
import com.dota2.common.utils.Result;
import com.dota2.entity.entity.SteamAccountEntity;
import com.dota2.entity.form.*;
import com.dota2.entity.service.SteamAccountService;
import com.dota2.entity.vo.SteamAccountVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/steamAccount")
@Validated
@Slf4j
@Api(tags = "Steam账号管理")
public class SteamAccountController {

    @Autowired
    private SteamAccountService steamAccountService;

    @Autowired
    private SteamSyncService steamSyncService;

    @ApiOperation(value = "分页查询Steam账号列表")
    @PostMapping(value = "/page")
    public Result<PageInfo<SteamAccountVo>> page(@RequestBody @Valid UserPageForm form) {
        PageHelper.startPage(form.getPage(), form.getSize());
        List<SteamAccountEntity> list = steamAccountService.list();
        PageInfo<SteamAccountEntity> pageInfo = new PageInfo<>(list);
        PageInfo<SteamAccountVo> voPage = new PageInfo<>();
        BeanUtils.copyProperties(pageInfo, voPage, "list");
        voPage.setList(pageInfo.getList().stream().map(entity -> {
            SteamAccountVo vo = new SteamAccountVo();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList()));
        return Result.ok(voPage);
    }

    @ApiOperation(value = "获取所有Steam账号(下拉用)")
    @GetMapping(value = "/listAll")
    public Result<List<SteamAccountEntity>> listAll() {
        return Result.ok(steamAccountService.list());
    }

    private static final long STEAM64_OFFSET = 76561197960265728L;

    private String toSteamId64(String inputId) {
        if (inputId == null || inputId.trim().isEmpty()) return "";
        String trimmed = inputId.trim();
        try {
            long id = Long.parseLong(trimmed);
            if (id < STEAM64_OFFSET) {
                return String.valueOf(id + STEAM64_OFFSET);
            }
        } catch (NumberFormatException ignored) {}
        return trimmed;
    }

    @ApiOperation(value = "从 OpenDota 同步 Steam 玩家信息（无需保存）", notes = "api.steam.account.sync")
    @GetMapping(value = "/sync/{steamId}")
    public Result<SteamPlayerDto> syncPlayerInfo(@PathVariable String steamId) {
        if (steamId == null || steamId.trim().isEmpty()) {
            return Result.fail("Steam ID 不能为空");
        }
        String targetSteamId = toSteamId64(steamId);
        // 检查是否已存在
        SteamAccountEntity existing = steamAccountService.lambdaQuery()
                .eq(SteamAccountEntity::getSteamId, targetSteamId)
                .one();
        if (existing != null) {
            SteamPlayerDto dto = new SteamPlayerDto();
            dto.setSteamId(existing.getSteamId());
            dto.setNickName(existing.getNickName());
            dto.setAvatar(existing.getAvatar());
            dto.setProfileUrl(existing.getProfileUrl());
            return Result.ok(dto);
        }

        SteamPlayerDto info = steamSyncService.syncFromOpenDota(steamId.trim());
        if (info == null) {
            return Result.fail("未找到该 Steam 账号，请检查 Steam ID 是否正确");
        }
        return Result.ok(info);
    }

    @ApiOperation(value = "同步并新增 Steam 账号（只需 steamId，其他自动填充）", notes = "api.steam.account.add")
    @PostMapping(value = "/addAccount")
    @SYSLOG
    public Result<String> addAccount(@RequestBody @Valid AddSteamAccountForm form) {
        String inputId = form.getSteamId().trim();
        String targetSteamId = toSteamId64(inputId);

        // 查重
        SteamAccountEntity exist = steamAccountService.lambdaQuery()
                .eq(SteamAccountEntity::getSteamId, targetSteamId)
                .one();
        if (exist != null) {
            throw new Dota2Exception("该 Steam 账号已存在");
        }

        // 自动同步
        SteamPlayerDto info = steamSyncService.syncFromOpenDota(inputId);
        if (info == null) {
            throw new Dota2Exception("未能同步到该 Steam 账号信息，请检查 Steam ID 是否正确");
        }

        SteamAccountEntity entity = new SteamAccountEntity();
        entity.setSteamId(info.getSteamId());
        entity.setNickName(info.getNickName());
        entity.setAvatar(info.getAvatar());
        entity.setProfileUrl(info.getProfileUrl());
        entity.setStatus(0);
        steamAccountService.save(entity);

        OperationLogHelper.setDetail(info.getNickName());
        log.info("新增Steam账号成功: steamId={}, nickName={}", info.getSteamId(), info.getNickName());
        return Result.ok();
    }

    @ApiOperation(value = "编辑Steam账号", notes = "api.steam.account.edit")
    @PostMapping(value = "/editAccount")
    @SYSLOG
    public Result<SteamAccountVo> editAccount(@RequestBody @Valid EditSteamAccountForm form) {
        SteamAccountEntity entity = steamAccountService.getById(form.getId());
        if (entity == null) {
            throw new Dota2Exception("Steam账号不存在");
        }
        OperationLogHelper.setDetail(entity.getNickName());
        if (form.getNickName() != null) entity.setNickName(form.getNickName());
        if (form.getAvatar() != null) entity.setAvatar(form.getAvatar());
        if (form.getProfileUrl() != null) entity.setProfileUrl(form.getProfileUrl());
        if (form.getStatus() != null) entity.setStatus(form.getStatus());
        steamAccountService.updateById(entity);
        SteamAccountVo vo = new SteamAccountVo();
        BeanUtils.copyProperties(entity, vo);
        return Result.ok(vo);
    }

    @ApiOperation(value = "删除Steam账号", notes = "api.steam.account.delete")
    @PostMapping(value = "/deleteAccount")
    @SYSLOG
    public Result<Boolean> deleteAccount(@RequestBody @Valid DeleteForm form) {
        List<SteamAccountEntity> accounts = steamAccountService.listByIds(form.getIds());
        String names = accounts.stream().map(SteamAccountEntity::getNickName).collect(Collectors.joining(", "));
        OperationLogHelper.setDetail(names);
        return Result.ok(steamAccountService.removeByIds(form.getIds()));
    }
}
