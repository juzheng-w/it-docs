package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 空间授权项
 */
@Data
@ApiModel(value = "AclDTO", description = "空间授权项")
public class AclDTO {

    @ApiModelProperty(value = "授权记录 ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "空间 ID", example = "1")
    private Long spaceId;
    @ApiModelProperty(value = "主体类型：USER / LDAP_GROUP", example = "USER")
    private String subjectType;
    @ApiModelProperty(value = "主体名称", example = "zhangsan")
    private String subjectName;
    @ApiModelProperty(value = "空间角色：EDITOR / VIEWER", example = "VIEWER")
    private String role;
    @ApiModelProperty(value = "授权操作人", example = "admin")
    private String createdBy;
    @ApiModelProperty(value = "授权时间", example = "2026-11-12 10:00:00")
    private Date createdAt;
}
