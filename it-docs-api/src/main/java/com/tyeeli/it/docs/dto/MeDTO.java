package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 当前用户信息（GET /api/users/me）：前端登录后拉一次，拿个人区域与可写空间
 */
@Data
@ApiModel(value = "MeDTO", description = "当前用户信息：前端登录后拉一次，拿个人区域与可读空间")
public class MeDTO {

    @ApiModelProperty(value = "用户 ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "用户名", example = "admin")
    private String username;
    @ApiModelProperty(value = "显示姓名", example = "管理员")
    private String displayName;
    @ApiModelProperty(value = "全局角色", example = "ADMIN")
    private String role;
    /**
     * LOCAL / LDAP：前端「个人中心」据此决定是否展示改密表单（域账号密码在 AD 管理）
     */
    @ApiModelProperty(value = "账号来源：LOCAL 本地 / LDAP 域", example = "LOCAL")
    private String authSource;
    /**
     * 个人区域（首次访问自动创建）
     */
    @ApiModelProperty(value = "个人区域（首次访问自动创建）")
    private SpaceDTO personalSpace;
    /**
     * 公共空间列表
     */
    @ApiModelProperty(value = "可读公共空间列表")
    private List<SpaceDTO> spaces;
}
