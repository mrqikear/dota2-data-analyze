package com.dota2.entity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("match_player")
public class MatchPlayerEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("match id")
    private Long matchId;

    @ApiModelProperty("Steam ID")
    private String steamId;

    @ApiModelProperty("hero id")
    private Integer heroId;

    @ApiModelProperty("kills")
    private Integer kills;

    @ApiModelProperty("deaths")
    private Integer deaths;

    @ApiModelProperty("assists")
    private Integer assists;

    @ApiModelProperty("win")
    private Boolean win;

    @ApiModelProperty("gpm")
    private Integer goldPerMin;

    @ApiModelProperty("xpm")
    private Integer xpPerMin;

    @ApiModelProperty("last hits")
    private Integer lastHits;

    @ApiModelProperty("denies")
    private Integer denies;

    @ApiModelProperty("hero damage")
    private Integer heroDamage;

    @ApiModelProperty("tower damage")
    private Integer towerDamage;

    @ApiModelProperty("healing")
    private Integer heroHealing;

    @ApiModelProperty("created time")
    private LocalDateTime createdTime;

    @ApiModelProperty("updated time")
    private LocalDateTime updatedTime;
}
