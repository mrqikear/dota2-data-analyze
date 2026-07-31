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
@TableName("match_detail")
public class MatchDetailEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("match id")
    @TableId(value = "match_id", type = IdType.INPUT)
    private Long matchId;

    @ApiModelProperty("radiant win")
    private Boolean radiantWin;

    @ApiModelProperty("duration seconds")
    private Integer duration;

    @ApiModelProperty("game mode")
    private Integer gameMode;

    @ApiModelProperty("lobby type")
    private Integer lobbyType;

    @ApiModelProperty("radiant score")
    private Integer radiantScore;

    @ApiModelProperty("dire score")
    private Integer direScore;

    @ApiModelProperty("radiant team name")
    private String radiantName;

    @ApiModelProperty("dire team name")
    private String direName;

    @ApiModelProperty("first blood time (seconds)")
    private Integer firstBloodTime;

    @ApiModelProperty("full OpenDota match JSON")
    private String rawJson;

    @ApiModelProperty("sync status: 0=pending, 1=in-progress, 2=done, -1=failed")
    private Integer syncStatus;

    @ApiModelProperty("sync error message")
    private String syncError;

    @ApiModelProperty("created time")
    private LocalDateTime createdTime;

    @ApiModelProperty("updated time")
    private LocalDateTime updatedTime;
}
