package com.dota2.entity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_constants")
public class GameConstantsEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String constType;
    private String dataJson;
    private Integer version;
    private LocalDateTime fetchedAt;
}
