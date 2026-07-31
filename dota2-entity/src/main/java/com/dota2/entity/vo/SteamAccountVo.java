package com.dota2.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SteamAccountVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "Steam ID")
    private String steamId;

    @ApiModelProperty(value = "昵称")
    private String nickName;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "个人资料URL")
    private String profileUrl;

    @ApiModelProperty(value = "最后拉取时间")
    private String lastFetchTime;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;
}
