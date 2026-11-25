package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 文档状态变更入参：上架 / 下架。
 * 只是状态翻转，不删任何数据（内容、版本、审计都保留）。
 */
@Data
@ApiModel(value = "DocStatusRequest", description = "文档状态变更：ACTIVE 上架 / DISABLED 下架")
public class DocStatusRequest {

    @ApiModelProperty(value = "目标状态：ACTIVE 上架 / DISABLED 下架", required = true, example = "DISABLED")
    @NotBlank(message = "状态不能为空")
    private String status;
}
