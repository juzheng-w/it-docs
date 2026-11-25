package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 从 AD 批量导入域账号（POST /api/users/import-ldap）：勾选的域用户 + 统一角色
 */
@Data
@ApiModel(value = "LdapImportRequest", description = "从 AD 导入：勾选的域用户列表 + 统一角色；已存在的自动跳过")
public class LdapImportRequest {

    @NotEmpty(message = "导入列表不能为空")
    @Valid
    private List<Item> users;

    @ApiModelProperty(value = "统一授予的角色，缺省 VIEWER", example = "VIEWER")
    private String role;

    @ApiModelProperty(value = "数据权限：可见业务系统列表；空/不传 = 全部系统", example = "[\"MES\"]")
    private List<String> sysScope;

    @Data
    @ApiModel(value = "LdapImportRequest.Item", description = "单个待导入域用户（字段来自 AD 搜索结果，原样回传）")
    public static class Item {

        @NotBlank(message = "username 不能为空")
        @ApiModelProperty(value = "域账号登录名", example = "zhangsan", required = true)
        private String username;

        @ApiModelProperty(value = "显示姓名", example = "张三")
        private String displayName;

        @ApiModelProperty(value = "邮箱", example = "zhangsan@corp.com")
        private String email;
    }
}
