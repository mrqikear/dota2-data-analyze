package com.dota2.entity.service.impl;

import com.dota2.common.base.CoreServiceImpl;
import com.dota2.entity.dao.MatchDetailDao;
import com.dota2.entity.entity.MatchDetailEntity;
import com.dota2.entity.service.MatchDetailService;
import org.springframework.stereotype.Service;

@Service
public class MatchDetailServiceImpl extends CoreServiceImpl<MatchDetailDao, MatchDetailEntity> implements MatchDetailService {
}
