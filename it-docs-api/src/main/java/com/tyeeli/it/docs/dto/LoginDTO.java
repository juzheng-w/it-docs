package com.tyeeli.it.docs.dto;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 登录响应：token + 展示用身份信息
 */
@Data
@ApiModel(value = "LoginDTO", description = "登录响应：token + 展示用身份信息")
public class LoginDTO {

    @ApiModelProperty(value = "JWT，后续请求放 Authorization: Bearer <token>", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;
    @ApiModelProperty(value = "用户名", example = "admin")
    private String username;
    @ApiModelProperty(value = "显示姓名", example = "管理员")
    private String displayName;
    @ApiModelProperty(value = "全局角色：ADMIN / EDITOR / VIEWER", example = "ADMIN")
    private String role;
    /**
     * 首登/重置后强制改密
     */
    @ApiModelProperty(value = "true 表示首登或重置后必须先改密", example = "false")
    private Boolean mustChangePwd;
}
