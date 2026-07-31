package com.dota2.entity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dota2.common.base.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("steam_account")
public class SteamAccountEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "Steam ID (64位数字ID)")
    private String steamId;

    @ApiModelProperty(value = "昵称")
    private String nickName;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "个人简介")
    private String profileUrl;

    @ApiModelProperty(value = "最后拉取比赛时间")
    private String lastFetchTime;

    @ApiModelProperty(value = "状态 0=正常 1=禁用")
    private Integer status;
}
