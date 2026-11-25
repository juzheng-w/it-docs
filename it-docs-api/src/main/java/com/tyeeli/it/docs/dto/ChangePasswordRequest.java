package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 本人改密（本地账号）
 */
@Data
@ApiModel(value = "ChangePasswordRequest", description = "本人修改密码，仅本地账号可用；域账号请到 AD 修改")
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    @ApiModelProperty( value = "原密码", example = "Itd@123456", required = true)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "新密码至少 8 位且含字母和数字")
    @ApiModelProperty( value = "新密码：至少 8 位，且同时包含字母与数字", example = "NewPass2026", required = true)
    private String newPassword;
}
