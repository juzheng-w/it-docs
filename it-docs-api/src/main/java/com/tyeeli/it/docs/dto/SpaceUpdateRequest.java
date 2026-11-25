package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新空间信息（编码不可改）
 */
@Data
@ApiModel(value = "SpaceUpdateRequest", description = "更新空间信息（编码不可改）")
public class SpaceUpdateRequest {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 50, message = "空间名称最长 50 字")
    @ApiModelProperty( value = "空间名称，最长 50 字", example = "采购仓储", required = true)
    private String name;

    @Size(max = 255, message = "描述最长 255 字")
    @ApiModelProperty(value = "描述，最长 255 字", example = "采购与仓储相关的操作手册")
    private String description;

    @ApiModelProperty(value = "可见性：PUBLIC / INTERNAL / RESTRICTED；空=不修改", example = "RESTRICTED",
            allowableValues = "PUBLIC,INTERNAL,RESTRICTED")
    private String visibility;
}
