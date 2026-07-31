package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

@Data
public class HeroStatsForm {

    @ApiModelProperty("Steam ID")
    private String steamId;

    @ApiModelProperty("match type: null=全部, 2=普通, 3=天梯, 4=加速模式")
    private Integer matchType;

    @ApiModelProperty("start time unix timestamp")
    private Long startTime;

    @ApiModelProperty("end time unix timestamp")
    private Long endTime;

    @ApiModelProperty("minimum matches to show (default 1)")
    @Min(0)
    private Integer minMatches = 1;

    @ApiModelProperty("sort field: games / winRate")
    private String sortField = "games";

    @ApiModelProperty("sort order: DESC / ASC")
    private String sortOrder = "DESC";
}
