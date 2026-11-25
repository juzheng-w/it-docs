package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 新建公共空间
 */
@Data
@ApiModel(value = "SpaceCreateRequest", description = "新建公共空间；编码创建后不可修改")
public class SpaceCreateRequest {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 50, message = "空间名称最长 50 字")
    @ApiModelProperty( value = "空间名称，最长 50 字", example = "采购仓储", required = true)
    private String name;

    @NotBlank(message = "空间编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$", message = "空间编码 2~50 位字母数字_-")
    @ApiModelProperty( value = "空间编码，2~50 位字母数字 _ -，创建后不可改", example = "purchase-wh", required = true)
    private String code;

    @Size(max = 255, message = "描述最长 255 字")
    @ApiModelProperty(value = "描述，最长 255 字", example = "采购与仓储相关的操作手册")
    private String description;

    @ApiModelProperty(value = "可见性：PUBLIC 匿名可看 / INTERNAL 登录可看 / RESTRICTED 仅授权名单；空=INTERNAL",
            example = "INTERNAL", allowableValues = "PUBLIC,INTERNAL,RESTRICTED")
    private String visibility;
}
