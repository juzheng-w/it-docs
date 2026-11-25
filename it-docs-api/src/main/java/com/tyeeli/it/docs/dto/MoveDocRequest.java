package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 文档换空间 / 个人区域发布到公共空间（PUT /docs/{id}/space）
 */
@Data
@ApiModel(value = "MoveDocRequest", description = "文档换空间 / 个人区域发布到公共空间")
public class MoveDocRequest {

    @NotNull(message = "目标空间不能为空")
    @ApiModelProperty( value = "目标空间 ID", example = "2", required = true)
    private Long spaceId;
}
