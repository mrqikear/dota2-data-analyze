package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ItemAnalysisForm {

    @ApiModelProperty("Steam ID (optional)")
    private String steamId;

    @ApiModelProperty("Hero ID (optional)")
    private Integer heroId;
}
