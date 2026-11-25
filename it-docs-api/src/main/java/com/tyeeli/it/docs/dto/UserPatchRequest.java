package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;

/**
 * 局部更新用户（PATCH 语义：只提交要改的字段，null 表示不改）。
 * 替代原 PUT /users/{id}/role 与 /users/{id}/status 两个动作式接口。
 */
@Data
@ApiModel(value = "UserPatchRequest", description = "局部更新成员：只提交要改的字段，未提交（null）表示保持不变")
public class UserPatchRequest {

    @Pattern(regexp = "^(ADMIN|EDITOR|VIEWER)$", message = "角色只能是 ADMIN/EDITOR/VIEWER")
    @ApiModelProperty(value = "全局角色", example = "EDITOR")
    private String role;

    @Pattern(regexp = "^(ACTIVE|DISABLED)$", message = "状态只能是 ACTIVE/DISABLED")
    @ApiModelProperty(value = "账号状态：DISABLED 后立即失去访问权", example = "ACTIVE")
    private String status;

    @ApiModelProperty(value = "显示姓名", example = "张三")
    private String displayName;

    @ApiModelProperty(value = "邮箱", example = "zhangsan@tyeeli.com")
    private String email;

    @ApiModelProperty(value = "数据权限：可见业务系统列表；null=不修改，空数组=恢复为全部系统", example = "[\"SVS\"]")
    private java.util.List<String> sysScope;
}
