package com.dota2.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dota2.entity.dao.GameConstantsDao;
import com.dota2.entity.entity.GameConstantsEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class GameConstantsService extends ServiceImpl<GameConstantsDao, GameConstantsEntity> {

    private static final String OPENDOTA_CONSTANTS = "https://api.opendota.com/api/constants";

    /** Auto-sync all constants daily at 4 AM */
    @Scheduled(cron = "0 0 4 * * ?")
    public void scheduledSync() {
        log.info("[GameConstants] ===== scheduled daily sync start =====");
        syncAll();
        log.info("[GameConstants] ===== scheduled daily sync done =====");
    }

    public void syncAll() {
        syncType("abilities", OPENDOTA_CONSTANTS + "/abilities");
        syncType("heroes", OPENDOTA_CONSTANTS + "/heroes");
        syncType("items", OPENDOTA_CONSTANTS + "/items");
        syncType("game_mode", OPENDOTA_CONSTANTS + "/game_mode");
        syncType("lobby_type", OPENDOTA_CONSTANTS + "/lobby_type");
        syncType("hero_lore", OPENDOTA_CONSTANTS + "/hero_lore");
        log.info("[GameConstants] all constants synced");
    }

    private void syncType(String type, String url) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(60000);
            String json = new RestTemplate(factory).getForObject(url, String.class);
            if (json == null || json.isEmpty()) {
                log.warn("[GameConstants] {} returned empty", type);
                return;
            }

            GameConstantsEntity entity = getOne(new LambdaQueryWrapper<GameConstantsEntity>()
                    .eq(GameConstantsEntity::getConstType, type));
            if (entity == null) {
                entity = new GameConstantsEntity();
                entity.setConstType(type);
                entity.setVersion(1);
            } else {
                entity.setVersion(entity.getVersion() + 1);
            }
            entity.setDataJson(json);
            entity.setFetchedAt(LocalDateTime.now());
            saveOrUpdate(entity, new LambdaQueryWrapper<GameConstantsEntity>()
                    .eq(GameConstantsEntity::getConstType, type));

            log.info("[GameConstants] synced {} ({} bytes)", type, json.length());
        } catch (Exception e) {
            log.warn("[GameConstants] failed to sync {}: {}", type, e.getMessage());
        }
    }

    /** Get cached data for a type */
    public GameConstantsEntity getCached(String type) {
        return getOne(new LambdaQueryWrapper<GameConstantsEntity>()
                .eq(GameConstantsEntity::getConstType, type));
    }
}
