package com.dota2.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MatchPlayerVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("ID")
    private Long id;

    @ApiModelProperty("match id")
    private Long matchId;

    @ApiModelProperty("Steam ID")
    private String steamId;

    @ApiModelProperty("Steam 昵称")
    private String nickName;

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

    @ApiModelProperty("start time")
    private Long startTime;

    @ApiModelProperty("duration")
    private Integer duration;

    @ApiModelProperty("game mode")
    private Integer gameMode;

    @ApiModelProperty("lobby type")
    private Integer lobbyType;

    @ApiModelProperty("created time")
    private LocalDateTime createdTime;

    @ApiModelProperty("whether match_detail has full parse data (damage_inflictor)")
    private Boolean parsed;

    @ApiModelProperty("MVP steam_id for this match")
    private String mvpSteamId;

    @ApiModelProperty("FMVP steam_id for this match")
    private String fmvpSteamId;
}
