package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试 LDAP：配置按提交值试连；填了 testUser/testPwd 走真实 bind
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "LdapTestRequest", description = "LDAP 测试连接：配置按提交值试连，填了 testUser/testPwd 走真实 bind")
public class LdapTestRequest extends LdapConfigRequest {

    @ApiModelProperty(value = "测试账号（不填只探测地址与 Base DN 连通性）", example = "zhangsan")
    private String testUser;

    @ApiModelProperty(value = "测试账号密码，仅本次 bind 使用，不落库", example = "Test@123")
    private String testPwd;
}
