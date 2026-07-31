package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class AddSteamAccountForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "Steam ID (64位数字ID)", required = true)
    @NotBlank(message = "Steam ID不能为空")
    private String steamId;
}
