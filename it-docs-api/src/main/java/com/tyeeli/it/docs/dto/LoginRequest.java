package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求
 */
@Data
@ApiModel(value = "LoginRequest", description = "登录凭证：本地账号与域账号同一入口，服务端按账号来源自动分流")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @ApiModelProperty( value = "用户名（域账号填 sAMAccountName，不带域后缀）", example = "admin", required = true)
    private String username;

    @NotBlank(message = "密码不能为空")
    @ApiModelProperty( value = "密码明文，服务端本地账号走 BCrypt、域账号走 LDAP bind，不本地存储", example = "admin@123", required = true)
    private String password;
}
