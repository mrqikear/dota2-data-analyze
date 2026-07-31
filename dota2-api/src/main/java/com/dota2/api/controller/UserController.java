package com.dota2.api.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.dota2.common.annotation.SYSLOG;
import com.dota2.common.exception.Dota2Exception;
import com.dota2.common.utils.JwtUtils;
import com.dota2.common.utils.OperationLogHelper;
import com.dota2.common.utils.Result;
import com.dota2.entity.entity.SysUserEntity;
import com.dota2.entity.form.*;
import com.dota2.entity.service.SysUserService;
import com.dota2.entity.vo.UserVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/user")
@Validated
@Slf4j
@Api(tags = "系统用户")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @ApiOperation(value = "登录", notes = "api.user.login")
    @PostMapping(value = "/login")
    @SYSLOG
    public Result<UserVo> login(@RequestBody @Valid LoginForm loginForm) {
        OperationLogHelper.setDetail(loginForm.getUserName());
        return Result.ok(sysUserService.login(loginForm));
    }

    @ApiOperation(value = "获取当前用户信息")
    @GetMapping(value = "/current")
    public Result<UserVo> getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("ssoToken");
        Long userId = JwtUtils.getUserId(token);
        return Result.ok(sysUserService.getCurrentUser(userId));
    }

    @ApiOperation(value = "分页查询用户列表")
    @PostMapping(value = "/page")
    public Result<PageInfo<SysUserEntity>> page(@RequestBody @Valid UserPageForm form) {
        PageHelper.startPage(form.getPage(), form.getSize());
        List<SysUserEntity> list = sysUserService.list();
        return Result.ok(new PageInfo<>(list));
    }

    @ApiOperation(value = "新增用户", notes = "api.user.add")
    @PostMapping(value = "/addUser")
    @SYSLOG
    public Result<String> addUser(@RequestBody @Valid AddUserForm form) {
        OperationLogHelper.setDetail(form.getUserName());
        SysUserEntity existing = sysUserService.findByUserName(form.getUserName());
        if (existing != null) {
            throw new Dota2Exception("用户名已存在");
        }
        SysUserEntity entity = new SysUserEntity();
        entity.setUserName(form.getUserName());
        entity.setPassWord(cn.hutool.crypto.digest.DigestUtil.md5Hex(form.getPassword()));
        entity.setNickName(form.getNickName());
        entity.setEmail(form.getEmail());
        entity.setPhone(form.getPhone());
        entity.setStatus(0);
        sysUserService.save(entity);
        return Result.ok();
    }

    @ApiOperation(value = "编辑用户", notes = "api.user.edit")
    @PostMapping(value = "/editUser")
    @SYSLOG
    public Result<SysUserEntity> editUser(@RequestBody @Valid EditUserForm form) {
        SysUserEntity entity = sysUserService.getById(form.getId());
        if (entity == null) {
            throw new Dota2Exception("用户不存在");
        }
        OperationLogHelper.setDetail(entity.getUserName());
        if (form.getNickName() != null) entity.setNickName(form.getNickName());
        if (form.getEmail() != null) entity.setEmail(form.getEmail());
        if (form.getPhone() != null) entity.setPhone(form.getPhone());
        if (form.getStatus() != null) entity.setStatus(form.getStatus());
        sysUserService.updateById(entity);
        return Result.ok(entity);
    }

    @ApiOperation(value = "删除用户", notes = "api.user.delete")
    @PostMapping(value = "/deleteUser")
    @SYSLOG
    public Result<Boolean> deleteUser(@RequestBody @Valid DeleteForm form) {
        List<SysUserEntity> users = sysUserService.listByIds(form.getIds());
        String names = users.stream().map(SysUserEntity::getUserName).collect(Collectors.joining(", "));
        OperationLogHelper.setDetail(names);
        return Result.ok(sysUserService.removeByIds(form.getIds()));
    }
}
