package com.dota2.entity.service.impl;

import com.dota2.common.base.CoreServiceImpl;
import com.dota2.entity.dao.SteamAccountDao;
import com.dota2.entity.entity.SteamAccountEntity;
import com.dota2.entity.service.SteamAccountService;
import org.springframework.stereotype.Service;

@Service
public class SteamAccountServiceImpl extends CoreServiceImpl<SteamAccountDao, SteamAccountEntity> implements SteamAccountService {
}
