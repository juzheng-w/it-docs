package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * LDAP 连接配置（管理界面提交；空串表示清空该键、回落 .env 默认值）
 */
@Data
@ApiModel(value = "LdapConfigRequest", description = "LDAP 连接配置；空串表示清空该键并回落默认值")
public class LdapConfigRequest {

    @ApiModelProperty(value = "LDAP 地址，如 ldap://ldap.example.com:389", example = "ldap://ldap.example.com:389")
    private String url;

    @ApiModelProperty(value = "Base DN，账号搜索起点", example = "dc=corp,dc=com")
    private String baseDn;

    /**
     * OpenLDAP 风格：uid={0},ou=people,dc=corp,dc=com
     */
    @ApiModelProperty(value = "OpenLDAP 风格 DN 模板，{0} 为登录名", example = "uid={0},ou=people,dc=corp,dc=com")
    private String userDnPattern;

    /**
     * AD 风格：{0}@corp.com
     */
    @ApiModelProperty(value = "AD 风格 UPN 模板，{0} 为登录名", example = "{0}@corp.com")
    private String upnPattern;

    @ApiModelProperty(value = "查询账号（UPN 或完整 DN），「从 AD 导入」搜索域用户用，登录 bind 不用它", example = "admin@corp.com")
    private String bindUser;

    @ApiModelProperty(value = "查询账号密码；读取接口不回显，留空=保持已保存值不变", example = "******")
    private String bindPassword;
}
