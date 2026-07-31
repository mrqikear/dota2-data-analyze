package com.dota2.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dota2.entity.dao.AssetCacheDao;
import com.dota2.entity.entity.AssetCacheEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
public class AssetCacheService extends ServiceImpl<AssetCacheDao, AssetCacheEntity> {

    @Autowired
    private RestTemplate restTemplate;

    public String getCached(String type, String key) {
        AssetCacheEntity e = getOne(new LambdaQueryWrapper<AssetCacheEntity>()
                .eq(AssetCacheEntity::getAssetType, type)
                .eq(AssetCacheEntity::getAssetKey, key));
        return e != null ? e.getBase64Data() : null;
    }

    public byte[] downloadAndCache(String type, String key, String url, String mimeType) {
        try {
            ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                byte[] data = resp.getBody();
                String b64 = Base64.getEncoder().encodeToString(data);

                AssetCacheEntity e = new AssetCacheEntity();
                e.setAssetType(type);
                e.setAssetKey(key);
                e.setMimeType(mimeType);
                e.setBase64Data(b64);
                e.setCreatedTime(LocalDateTime.now());
                save(e);

                log.info("[AssetCache] cached {} {} ({} bytes)", type, key, data.length);
                return data;
            }
        } catch (Exception ex) {
            log.warn("[AssetCache] download failed for {} {}: {}", type, key, ex.getMessage());
        }
        return new byte[0];
    }
}
