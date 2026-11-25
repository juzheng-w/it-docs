package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 操作日志（含改前改后快照）
 */
@Data
@ApiModel(value = "OpLogDTO", description = "操作日志（含改前改后快照）")
public class OpLogDTO {

    @ApiModelProperty(value = "日志 ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "操作类型：CREATE / UPDATE / DELETE / MOVE / RESTORE", example = "UPDATE")
    private String opType;
    @ApiModelProperty(value = "文档 ID", example = "1")
    private Long docId;
    @ApiModelProperty(value = "操作人", example = "admin")
    private String operator;
    @ApiModelProperty(value = "改前快照（JSON）", example = "{\"title\":\"旧标题\"}")
    private String beforeJson;
    @ApiModelProperty(value = "改后快照（JSON）", example = "{\"title\":\"新标题\"}")
    private String afterJson;
    @ApiModelProperty(value = "操作时间", example = "2026-11-12 10:00:00")
    private Date opTime;
}
