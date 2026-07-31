package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserPageForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "用户名(模糊搜索)")
    private String userName;

    @ApiModelProperty(value = "状态")
    private Integer status;
}
