package com.dota2.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class HeroStatsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("hero id")
    private Integer heroId;

    @ApiModelProperty("total games played")
    private Long games;

    @ApiModelProperty("overall win rate (%)")
    private Double winRate;

    @ApiModelProperty("average kills")
    private Double avgKills;

    @ApiModelProperty("average deaths")
    private Double avgDeaths;

    @ApiModelProperty("average assists")
    private Double avgAssists;

    @ApiModelProperty("average KDA ratio")
    private Double avgKda;

    @ApiModelProperty("average GPM")
    private Double avgGoldPerMin;

    @ApiModelProperty("average XPM")
    private Double avgXpPerMin;

    // ---- Recent 20 ----
    @ApiModelProperty("recent 20 games count")
    private Long recentGames20;

    @ApiModelProperty("recent 20 win rate")
    private Double recentWinRate20;

    @ApiModelProperty("recent 20 avg kills")
    private Double recentKills20;

    @ApiModelProperty("recent 20 avg deaths")
    private Double recentDeaths20;

    @ApiModelProperty("recent 20 avg assists")
    private Double recentAssists20;

    @ApiModelProperty("recent 20 KDA")
    private Double recentKda20;

    // ---- Recent 50 ----
    @ApiModelProperty("recent 50 games count")
    private Long recentGames50;

    @ApiModelProperty("recent 50 win rate")
    private Double recentWinRate50;

    @ApiModelProperty("recent 50 avg kills")
    private Double recentKills50;

    @ApiModelProperty("recent 50 avg deaths")
    private Double recentDeaths50;

    @ApiModelProperty("recent 50 avg assists")
    private Double recentAssists50;

    @ApiModelProperty("recent 50 KDA")
    private Double recentKda50;
}
