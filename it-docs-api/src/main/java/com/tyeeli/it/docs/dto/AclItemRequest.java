package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 空间 ACL 单条授权（PUT /spaces/{id}/acl 提交整个数组，全量覆盖）
 */
@Data
@ApiModel(value = "AclItemRequest", description = "空间 ACL 单条授权（PUT /spaces/{id}/acl 提交整个数组，全量覆盖）")
public class AclItemRequest {

    @NotBlank(message = "主体类型不能为空")
    @Pattern(regexp = "^(USER|LDAP_GROUP)$", message = "主体类型只能是 USER/LDAP_GROUP")
    @ApiModelProperty( value = "主体类型：USER 用户 / LDAP_GROUP 域组", example = "USER", required = true)
    private String subjectType;

    @NotBlank(message = "主体名称不能为空")
    @ApiModelProperty( value = "主体名称：USER 填用户名，LDAP_GROUP 填域组名", example = "zhangsan", required = true)
    private String subjectName;

    @NotBlank(message = "空间角色不能为空")
    @Pattern(regexp = "^(EDITOR|VIEWER)$", message = "空间角色只能是 EDITOR/VIEWER")
    @ApiModelProperty( value = "空间角色：EDITOR 可写，VIEWER 只读", example = "VIEWER", required = true)
    private String role;
}
