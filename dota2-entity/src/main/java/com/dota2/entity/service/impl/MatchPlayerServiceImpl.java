package com.dota2.entity.service.impl;

import com.dota2.common.base.CoreServiceImpl;
import com.dota2.entity.dao.MatchPlayerDao;
import com.dota2.entity.entity.MatchPlayerEntity;
import com.dota2.entity.service.MatchPlayerService;
import org.springframework.stereotype.Service;

@Service
public class MatchPlayerServiceImpl extends CoreServiceImpl<MatchPlayerDao, MatchPlayerEntity> implements MatchPlayerService {
}
