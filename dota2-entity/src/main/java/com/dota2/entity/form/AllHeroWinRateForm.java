package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AllHeroWinRateForm {

    @ApiModelProperty("days: 3 / 5 / 7 / null=全部")
    private Integer days;

    @ApiModelProperty("game mode: turbo / pub / null=全部")
    private String gameMode;
}
