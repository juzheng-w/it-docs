package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 空间（PUBLIC 公共 / PERSONAL 个人）
 */
@Data
@ApiModel(value = "SpaceDTO", description = "空间：PUBLIC 公共空间 / PERSONAL 个人区域")
public class SpaceDTO {

    @ApiModelProperty(value = "空间 ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "空间名称", example = "采购仓储")
    private String spaceName;
    @ApiModelProperty(value = "空间编码，全局唯一", example = "purchase-wh")
    private String spaceCode;
    @ApiModelProperty(value = "空间类型：PUBLIC 公共 / PERSONAL 个人", example = "PUBLIC")
    private String spaceType;
    @ApiModelProperty(value = "可见性：PUBLIC 匿名 / INTERNAL 登录 / RESTRICTED 名单（仅公共空间）", example = "INTERNAL")
    private String visibility;
    @ApiModelProperty(value = "描述", example = "采购与仓储相关的操作手册")
    private String description;
    @ApiModelProperty(value = "属主（个人区域为用户名，公共空间为空）", example = "admin")
    private String owner;
    @ApiModelProperty(value = "状态：ACTIVE / DISABLED", example = "ACTIVE")
    private String status;
    @ApiModelProperty(value = "创建时间", example = "2026-11-12 10:00:00")
    private Date createdAt;
}
