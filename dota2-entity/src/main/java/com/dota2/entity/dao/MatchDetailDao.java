package com.dota2.entity.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dota2.entity.entity.MatchDetailEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MatchDetailDao extends BaseMapper<MatchDetailEntity> {

    List<Long> selectMissingDetailIds(@Param("limit") int limit);

    long countMissingDetailIds();
}
