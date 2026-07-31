package com.dota2.entity.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DeleteForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "要删除的ID列表", required = true)
    private List<Long> ids;
}
