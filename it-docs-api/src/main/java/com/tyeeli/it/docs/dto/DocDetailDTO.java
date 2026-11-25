package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 文档详情（含正文），阅读页与编辑器用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "DocDetailDTO", description = "文档详情：列表字段 + 正文，阅读页与编辑器用")
public class DocDetailDTO extends DocDTO {

    /**
     * Markdown 正文
     */
    @ApiModelProperty(value = "Markdown 正文", example = "## 现象：提交入库单报错")
    private String contentMd;

    /**
     * 问题记录结构化内容（JSON 字符串）
     */
    @ApiModelProperty(value = "问题记录结构化内容（JSON 字符串）", example = "{\"errorText\":\"…\"}")
    private String contentJson;

    @ApiModelProperty(value = "创建人", example = "admin")
    private String createdBy;
    @ApiModelProperty(value = "创建时间", example = "2026-11-12 10:00:00")
    private Date createdAt;
}
