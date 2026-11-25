package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV 导入结果：成功/更新/跳过计数 + 前若干条错误明细（按行号）。
 */
@Data
@ApiModel(value = "DocImportResultDTO", description = "CSV 导入结果：total/created/updated/skipped/errors")
public class DocImportResultDTO {

    @ApiModelProperty(value = "数据行总数（不含表头）", example = "12")
    private int total;

    @ApiModelProperty(value = "新建条数", example = "10")
    private int created;

    @ApiModelProperty(value = "按编号/标题更新条数", example = "1")
    private int updated;

    @ApiModelProperty(value = "跳过条数（重复或空行）", example = "1")
    private int skipped;

    @ApiModelProperty(value = "失败明细，最多 20 条", example = "[\"第 3 行：标题不能为空\"]")
    private List<String> errors = new ArrayList<String>();
}
