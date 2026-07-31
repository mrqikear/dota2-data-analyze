package com.dota2.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class StartingItemComboVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("item internal names (e.g. branches, magic_stick)")
    private List<String> items;

    @ApiModelProperty("number of games with this combo")
    private Long games;

    @ApiModelProperty("win rate (%)")
    private Double winRate;

    @ApiModelProperty("number of wins")
    private Long wins;
}
