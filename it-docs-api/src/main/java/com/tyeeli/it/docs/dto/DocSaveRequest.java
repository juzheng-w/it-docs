package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 新建 / 更新文档（问题记录、业务文档、技术文档及旧版 md 文档）共用。
 * 只暴露业务字段：docNo、status、createdBy、createdAt 等由服务端生成，客户端传了也无效。
 */
@Data
@ApiModel(value = "DocSaveRequest", description = "新建 / 更新文档共用；docNo、状态、创建人由服务端生成，传了也无效")
public class DocSaveRequest {

    /**
     * 新建必填；更新时忽略（换空间走 PUT /docs/{id}/space）
     */
    @ApiModelProperty(value = "目标空间 ID；更新时忽略（换空间走 PUT /api/docs/{id}/space）", example = "1")
    private Long spaceId;

    /**
     * qa / req / tech；md 兼容旧数据，缺省 tech
     */
    @ApiModelProperty(value = "文档类型：qa / req / tech；md 仅兼容旧数据", example = "tech")
    private String docType;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字")
    @ApiModelProperty( value = "标题，最长 200 字", example = "WMS 入库单创建报错排查", required = true)
    private String title;

    /**
     * 工单归属系统（WMS/ERP/SRM…），不是处理落点
     */
    @ApiModelProperty(value = "工单归属系统（WMS/ERP/SRM…），不是处理落点", example = "WMS")
    private String system;

    @ApiModelProperty(value = "文档分类", example = "采购入库")
    private String category;

    @ApiModelProperty(value = "业务责任人（可与创建人不同）", example = "张三")
    private String owner;

    @Size(max = 500, message = "摘要最长 500 字")
    @ApiModelProperty(value = "摘要 / 一句话答案，最长 500 字，列表与阅读页展示", example = "入库单提交后提示供应商不存在")
    private String summary;

    /**
     * Markdown 正文
     */
    @ApiModelProperty(value = "Markdown 正文", example = "## 现象：提交入库单报错")
    private String contentMd;

    /**
     * 问题记录结构化内容（JSON 字符串）
     */
    @ApiModelProperty(value = "问题记录结构化内容（JSON 字符串），docType=qa 时使用", example = "{\"errorText\":\"…\"}")
    private String contentJson;

    /**
     * 逗号分隔标签
     */
    @ApiModelProperty(value = "逗号分隔标签", example = "入库,供应商,常见问题")
    private String tags;
}
