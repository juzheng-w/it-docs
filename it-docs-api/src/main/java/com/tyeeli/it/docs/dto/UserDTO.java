package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 成员（不含 passwordHash 等敏感字段）
 */
@Data
@ApiModel(value = "UserDTO", description = "成员信息（不含 passwordHash 等敏感字段）")
public class UserDTO {

    @ApiModelProperty(value = "用户 ID", example = "2")
    private Long id;
    @ApiModelProperty(value = "用户名", example = "zhangsan")
    private String username;
    @ApiModelProperty(value = "显示姓名", example = "张三")
    private String displayName;
    @ApiModelProperty(value = "账号来源：LOCAL / LDAP", example = "LOCAL")
    private String authSource;
    @ApiModelProperty(value = "全局角色", example = "VIEWER")
    private String role;
    @ApiModelProperty(value = "数据权限：可见业务系统列表；空 = 全部系统", example = "[\"SVS\",\"OTWB\"]")
    private java.util.List<String> sysScope;
    @ApiModelProperty(value = "邮箱", example = "zhangsan@tyeeli.com")
    private String email;
    @ApiModelProperty(value = "状态：ACTIVE / DISABLED", example = "ACTIVE")
    private String status;
    @ApiModelProperty(value = "是否需要首次登录改密", example = "true")
    private Boolean mustChangePwd;
    @ApiModelProperty(value = "最后登录时间", example = "2026-11-12 09:30:00")
    private Date lastLoginAt;
    @ApiModelProperty(value = "创建时间", example = "2026-11-10 14:00:00")
    private Date createdAt;
    /**
     * 仅在「创建账号」响应里出现一次
     */
    @ApiModelProperty(value = "一次性临时密码，只在「创建账号」响应里出现一次", example = "Tmp@8f3k2")
    private String tempPassword;
}
