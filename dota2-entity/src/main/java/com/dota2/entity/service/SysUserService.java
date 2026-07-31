package com.dota2.entity.service;

import com.dota2.common.base.CoreService;
import com.dota2.entity.entity.SysUserEntity;
import com.dota2.entity.vo.UserVo;
import com.dota2.entity.form.LoginForm;

public interface SysUserService extends CoreService<SysUserEntity> {

    UserVo login(LoginForm loginForm);

    UserVo getCurrentUser(Long userId);

    SysUserEntity findByUserName(String userName);
}
