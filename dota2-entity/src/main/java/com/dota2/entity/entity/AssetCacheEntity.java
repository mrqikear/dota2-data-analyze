package com.dota2.entity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("asset_cache")
public class AssetCacheEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetType;
    private String assetKey;
    private String mimeType;
    private String base64Data;
    private LocalDateTime createdTime;
}
