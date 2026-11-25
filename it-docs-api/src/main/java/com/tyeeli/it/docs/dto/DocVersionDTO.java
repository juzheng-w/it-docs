package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 版本快照（列表不含正文，回滚按 versionNo）
 */
@Data
@ApiModel(value = "DocVersionDTO", description = "版本快照（列表不含正文，回滚按 versionNo）")
public class DocVersionDTO {

    @ApiModelProperty(value = "版本记录 ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "文档 ID", example = "1")
    private Long docId;
    @ApiModelProperty(value = "版本号，从 1 递增", example = "3")
    private Integer versionNo;
    @ApiModelProperty(value = "快照结构版本：1=旧快照（仅标题与正文），2=完整内容快照", example = "2")
    private Integer snapshotV;
    @ApiModelProperty(value = "该版本标题快照", example = "WMS 入库单创建报错排查")
    private String title;
    @ApiModelProperty(value = "该版本保存人", example = "admin")
    private String operator;
    @ApiModelProperty(value = "快照时间", example = "2026-11-12 10:00:00")
    private Date createdAt;
}
