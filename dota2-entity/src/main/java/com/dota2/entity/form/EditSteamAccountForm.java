package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class EditSteamAccountForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "Steam账号ID", required = true)
    @NotNull(message = "ID不能为空")
    private Long id;

    @ApiModelProperty(value = "昵称")
    private String nickName;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "个人资料页URL")
    private String profileUrl;

    @ApiModelProperty(value = "状态 0=正常 1=禁用")
    private Integer status;
}
