package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 字典项新增/编辑入参。新增时必填 type；编辑走 PUT /meta/{id}，type 不可改。
 */
@Data
@ApiModel(value = "MetaSaveRequest", description = "字典项新增/编辑入参")
public class MetaSaveRequest {

    @ApiModelProperty(value = "类型 BIZ_SYSTEM/CATEGORY（新增必填，编辑忽略）", example = "CATEGORY")
    @Size(max = 20, message = "类型长度超限")
    private String type;

    @ApiModelProperty(value = "归属业务系统字典 id（仅 CATEGORY 用；空 = 通用分类，所有系统可选）", example = "1")
    private Long parentId;

    @ApiModelProperty(value = "名称，同类型内唯一", example = "高频", required = true)
    @NotBlank(message = "名称不能为空")
    @Size(max = 50, message = "名称最多 50 字")
    private String name;

    @ApiModelProperty(value = "展示色 #RRGGBB，可空", example = "#F56C6C")
    @Size(max = 10, message = "颜色格式：#RRGGBB")
    private String color;

    @ApiModelProperty(value = "排序号，小的在前，默认 0", example = "1")
    private Integer sortNo;
}
