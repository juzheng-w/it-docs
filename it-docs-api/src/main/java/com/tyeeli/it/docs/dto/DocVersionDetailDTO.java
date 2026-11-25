package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单个历史版本的完整内容，用于版本回看与回滚前的预览。
 * 版本列表仍返回不含正文的 {@link DocVersionDTO}，避免一次拉取全部正文。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "DocVersionDetailDTO", description = "历史版本详情：版本元信息 + 正文快照")
public class DocVersionDetailDTO extends DocVersionDTO {

    @ApiModelProperty(value = "该版本的一句话答案快照", example = "入库单提交后提示供应商不存在")
    private String summary;

    @ApiModelProperty(value = "该版本的归属业务系统快照", example = "WMS")
    private String system;

    @ApiModelProperty(value = "该版本的问题分类 / 业务场景快照", example = "采购入库")
    private String category;

    @ApiModelProperty(value = "该版本的处理角色快照", example = "仓库管理员")
    private String owner;

    @ApiModelProperty(value = "该版本的标签快照（英文逗号分隔）", example = "入库,供应商")
    private String tags;

    @ApiModelProperty(value = "该版本的 Markdown 正文快照", example = "## 现象：提交入库单报错")
    private String contentMd;

    @ApiModelProperty(value = "该版本的问题记录结构化内容快照（qa）", example = "{\"errorText\":\"供应商不存在\"}")
    private String contentJson;
}
