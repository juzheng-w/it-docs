package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@ApiModel(value = "DocAclRequest", description = "单篇文档读取限制主体")
public class DocAclRequest {
    @NotBlank
    @Pattern(regexp = "^(USER|LDAP_GROUP)$")
    @ApiModelProperty(value = "USER 或 LDAP_GROUP", required = true)
    private String subjectType;
    @NotBlank
    @Size(max = 100)
    @ApiModelProperty(value = "用户名或 LDAP 组名称", required = true)
    private String subjectName;
}
