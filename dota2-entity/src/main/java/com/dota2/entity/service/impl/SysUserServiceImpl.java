package com.dota2.entity.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dota2.common.base.CoreServiceImpl;
import com.dota2.common.exception.Dota2Exception;
import com.dota2.common.utils.JwtUtils;
import com.dota2.entity.dao.SysUserDao;
import com.dota2.entity.entity.SysUserEntity;
import com.dota2.entity.form.LoginForm;
import com.dota2.entity.service.SysUserService;
import com.dota2.entity.vo.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SysUserServiceImpl extends CoreServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {

    @Override
    public UserVo login(LoginForm loginForm) {
        SysUserEntity user = findByUserName(loginForm.getUserName());
        if (user == null) {
            throw new Dota2Exception("用户名或密码错误");
        }

        String encryptedPassword = DigestUtil.md5Hex(loginForm.getPassword());
        log.info("Login debug: inputRaw={}, inputMd5={}, dbPassword={}", loginForm.getPassword(), encryptedPassword, user.getPassWord());
        if (!encryptedPassword.equalsIgnoreCase(user.getPassWord())) {
            throw new Dota2Exception("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new Dota2Exception("账号已被禁用");
        }

        String token = JwtUtils.createToken(user.getId(), user.getUserName());

        UserVo vo = new UserVo();
        BeanUtils.copyProperties(user, vo);
        vo.setToken(token);
        return vo;
    }

    @Override
    public UserVo getCurrentUser(Long userId) {
        SysUserEntity user = getById(userId);
        if (user == null) {
            return null;
        }
        UserVo vo = new UserVo();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public SysUserEntity findByUserName(String userName) {
        return getOne(Wrappers.<SysUserEntity>lambdaQuery()
                .eq(SysUserEntity::getUserName, userName));
    }
}
