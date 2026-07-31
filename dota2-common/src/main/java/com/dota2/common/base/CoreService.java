package com.dota2.common.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CoreService<T> extends IService<T> {

    List<T> findByKv(Object... param);

    T selectOne(Wrapper<T> wrapper);

    List<T> selectList(Wrapper<T> wrapper);
}
