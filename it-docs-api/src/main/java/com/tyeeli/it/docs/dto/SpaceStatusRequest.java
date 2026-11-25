package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 空间停用 / 启用（PATCH /api/spaces/{id}/status）
 */
@Data
@ApiModel(value = "SpaceStatusRequest", description = "空间停用 / 启用请求")
public class SpaceStatusRequest {

    @NotBlank(message = "status 不能为空")
    @Pattern(regexp = "^(ACTIVE|DISABLED)$", message = "status 只能是 ACTIVE/DISABLED")
    @ApiModelProperty(value = "目标状态：DISABLED 停用（列表消失、数据保留）/ ACTIVE 恢复", example = "DISABLED",
            required = true, allowableValues = "ACTIVE,DISABLED")
    private String status;
}
