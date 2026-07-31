package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class MatchPageForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页条数", example = "100")
    private Integer size = 100;

    @ApiModelProperty(value = "Steam ID 筛选")
    private String steamId;

    @ApiModelProperty(value = "游戏模式筛选")
    private Integer gameMode;

    @ApiModelProperty(value = "英雄 ID 筛选")
    private Integer heroId;

    @ApiModelProperty(value = "排序字段")
    private String sortField;

    @ApiModelProperty(value = "排序方向 asc/desc")
    private String sortOrder;

    @ApiModelProperty(value = "是否已解析（有 damage_inflictor 数据）")
    private Boolean parsed;
}
