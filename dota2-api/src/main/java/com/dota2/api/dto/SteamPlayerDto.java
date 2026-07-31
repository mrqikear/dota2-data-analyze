package com.dota2.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Steam 玩家信息 DTO（从 OpenDota API 同步）
 */
@Data
@ApiModel("Steam玩家信息")
public class SteamPlayerDto {
    @ApiModelProperty("Steam64 ID")
    private String steamId;

    @ApiModelProperty("昵称")
    private String nickName;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("个人资料页URL")
    private String profileUrl;

    @ApiModelProperty("国家代码")
    private String countryCode;
}
