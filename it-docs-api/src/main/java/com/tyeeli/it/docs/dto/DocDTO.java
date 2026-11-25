package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 文档列表项（不含正文，控制列表响应体积）
 */
@Data
@ApiModel(value = "DocDTO", description = "文档列表项（不含正文，控制列表响应体积）")
public class DocDTO {

    @ApiModelProperty(value = "文档 ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "文档编号，服务端按类型前缀 + 年月 + 月内序号生成", example = "QA-2026110001")
    private String docNo;
    @ApiModelProperty(value = "所属空间 ID", example = "1")
    private Long spaceId;
    @ApiModelProperty(value = "文档类型：qa / req / tech / md（旧版）", example = "tech")
    private String docType;
    @ApiModelProperty(value = "标题", example = "WMS 入库单创建报错排查")
    private String title;
    @ApiModelProperty(value = "归属系统", example = "WMS")
    private String system;
    @ApiModelProperty(value = "文档分类", example = "采购入库")
    private String category;
    @ApiModelProperty(value = "业务责任人", example = "张三")
    private String owner;
    @ApiModelProperty(value = "摘要", example = "入库单提交后提示供应商不存在")
    private String summary;
    @ApiModelProperty(value = "逗号分隔标签", example = "入库,供应商")
    private String tags;
    @ApiModelProperty(value = "状态：ACTIVE / DISABLED / DELETED", example = "ACTIVE")
    private String status;
    @ApiModelProperty(value = "最后修改人", example = "admin")
    private String updatedBy;
    @ApiModelProperty(value = "最后修改时间", example = "2026-11-12 10:00:00")
    private Date updatedAt;
}
