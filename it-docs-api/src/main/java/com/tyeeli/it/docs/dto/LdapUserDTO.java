package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * AD 域用户搜索结果（GET /api/users/ldap）：exists 标记是否已在本库建档
 */
@Data
@ApiModel(value = "LdapUserDTO", description = "AD 域用户搜索结果；exists=true 表示本库已有同名账号，导入时会跳过")
public class LdapUserDTO {

    @ApiModelProperty(value = "域账号登录名（sAMAccountName）", example = "zhangsan", required = true)
    private String username;

    @ApiModelProperty(value = "显示姓名", example = "张三")
    private String displayName;

    @ApiModelProperty(value = "邮箱（无 mail 时取 UPN）", example = "zhangsan@corp.com")
    private String email;

    @ApiModelProperty(value = "本库是否已存在同名账号", example = "false", required = true)
    private Boolean exists;
}
