package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 管理员创建账号（本地 / 域预建）
 */
@Data
@ApiModel(value = "UserCreateRequest", description = "管理员创建账号；LOCAL 创建后为「首次登录须改密」并返回临时密码，LDAP 为预建域档案（无密码，用户直接用 AD 密码登录）")
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,50}$", message = "用户名 3~50 位，仅字母数字._-")
    @ApiModelProperty( value = "用户名，3~50 位，仅字母数字与 . _ -", example = "zhangsan", required = true)
    private String username;

    @ApiModelProperty(value = "账号来源：LOCAL 本地（默认）/ LDAP 域预建（不设密码，用户用 AD 密码登录）", example = "LOCAL")
    private String authSource;

    @ApiModelProperty(value = "显示姓名（不填默认与用户名一致）", example = "张三")
    private String displayName;

    /**
     * ADMIN / EDITOR / VIEWER，缺省 VIEWER
     */
    @ApiModelProperty(value = "全局角色，缺省 VIEWER", example = "EDITOR")
    private String role;

    @Email(message = "邮箱格式不正确")
    @ApiModelProperty(value = "邮箱，可为空", example = "zhangsan@tyeeli.com")
    private String email;

    @ApiModelProperty(value = "数据权限：可见业务系统列表；空/不传 = 全部系统", example = "[\"SVS\",\"OTWB\"]")
    private java.util.List<String> sysScope;
}
