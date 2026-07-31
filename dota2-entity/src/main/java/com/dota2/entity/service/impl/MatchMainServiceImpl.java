package com.dota2.entity.service.impl;

import com.dota2.common.base.CoreServiceImpl;
import com.dota2.entity.dao.MatchMainDao;
import com.dota2.entity.entity.MatchMainEntity;
import com.dota2.entity.service.MatchMainService;
import org.springframework.stereotype.Service;

@Service
public class MatchMainServiceImpl extends CoreServiceImpl<MatchMainDao, MatchMainEntity> implements MatchMainService {
}
