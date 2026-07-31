package com.dota2.api.service;

import com.dota2.api.dto.SteamPlayerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Steam 账号同步服务 — 通过 OpenDota API 自动获取玩家信息
 */
@Slf4j
@Service
public class SteamSyncService {

    private static final long STEAM64_OFFSET = 76561197960265728L;
    private static final String OPENDOTA_PLAYER_URL = "https://api.opendota.com/api/players/{accountId}";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 通过 OpenDota API 同步 Steam 玩家信息
     * 支持传入 64 位 SteamID (如 76561198086683738) 或 32 位 AccountID (如 126418010)
     */
    @SuppressWarnings("unchecked")
    public SteamPlayerDto syncFromOpenDota(String inputId) {
        if (inputId == null || inputId.trim().isEmpty()) {
            return null;
        }
        String trimmed = inputId.trim();
        String accountId = trimmed;
        String defaultSteamId64 = trimmed;

        try {
            long id = Long.parseLong(trimmed);
            if (id > STEAM64_OFFSET) {
                accountId = String.valueOf(id - STEAM64_OFFSET);
                defaultSteamId64 = trimmed;
            } else {
                accountId = trimmed;
                defaultSteamId64 = String.valueOf(id + STEAM64_OFFSET);
            }
        } catch (NumberFormatException ignored) {}

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(OPENDOTA_PLAYER_URL, Map.class, accountId);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("OpenDota API 返回非正常状态: {}", resp.getStatusCode());
                return null;
            }
            Map<String, Object> profile = (Map<String, Object>) resp.getBody().get("profile");
            if (profile == null) {
                log.warn("OpenDota API 返回数据中无 profile 字段");
                return null;
            }

            SteamPlayerDto dto = new SteamPlayerDto();
            String profileSteamId = safeStr(profile.get("steamid"));
            dto.setSteamId(profileSteamId.isEmpty() ? defaultSteamId64 : profileSteamId);
            dto.setNickName(safeStr(profile.get("personaname")));
            dto.setAvatar(safeStr(profile.get("avatarfull")));

            String profileUrl = safeStr(profile.get("profileurl"));
            if (profileUrl.isEmpty()) {
                profileUrl = "https://steamcommunity.com/profiles/" + dto.getSteamId();
            }
            dto.setProfileUrl(profileUrl);
            dto.setCountryCode(safeStr(profile.get("loccountrycode")));
            log.info("同步成功: inputId={}, accountId={}, steamId={}, nickName={}", inputId, accountId, dto.getSteamId(), dto.getNickName());
            return dto;
        } catch (Exception e) {
            log.error("OpenDota API 同步失败: inputId={}, accountId={}, error={}", inputId, accountId, e.getMessage());
            return null;
        }
    }

    private String safeStr(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}

