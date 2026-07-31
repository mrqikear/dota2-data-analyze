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
@TableName("match_main")
public class MatchMainEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("match id (OpenDota)")
    @TableId(value = "match_id", type = IdType.INPUT)
    private Long matchId;

    @ApiModelProperty("start time (unix)")
    private Long startTime;

    @ApiModelProperty("duration seconds")
    private Integer duration;

    @ApiModelProperty("game mode")
    private Integer gameMode;

    @ApiModelProperty("lobby type")
    private Integer lobbyType;

    @ApiModelProperty("created time")
    private LocalDateTime createdTime;

    @ApiModelProperty("updated time")
    private LocalDateTime updatedTime;

    @ApiModelProperty("deleted")
    private Integer deleted;

    @ApiModelProperty("MVP player steam_id (winning team's best)")
    private String mvpSteamId;

    @ApiModelProperty("FMVP player steam_id (losing team's best)")
    private String fmvpSteamId;
}
