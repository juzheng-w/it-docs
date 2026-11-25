package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 字典项出参（业务系统/问题分类）。
 */
@Data
@ApiModel(value = "MetaDTO", description = "字典项：id/type/name/color/sortNo")
public class MetaDTO {

    @ApiModelProperty(value = "字典项 ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "类型", example = "BIZ_SYSTEM")
    private String type;

    @ApiModelProperty(value = "父级（仅分类：所属业务系统字典 id；空=通用）", example = "1")
    private Long parentId;

    @ApiModelProperty(value = "名称（文档里存的就是它）", example = "SVS")
    private String name;

    @ApiModelProperty(value = "展示色 #RRGGBB，空则前端默认色", example = "#409EFF")
    private String color;

    @ApiModelProperty(value = "排序号，小的在前", example = "1")
    private Integer sortNo;
}
