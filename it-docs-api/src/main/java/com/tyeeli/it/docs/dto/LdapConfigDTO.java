package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 当前生效的 LDAP 配置（不含任何密码）
 */
@Data
@ApiModel(value = "LdapConfigDTO", description = "当前生效的 LDAP 配置（不含任何密码）")
public class LdapConfigDTO {

    @ApiModelProperty(value = "LDAP 地址", example = "ldap://ldap.example.com:389")
    private String url;
    @ApiModelProperty(value = "Base DN", example = "dc=corp,dc=com")
    private String baseDn;
    @ApiModelProperty(value = "OpenLDAP 风格 DN 模板", example = "uid={0},ou=people,dc=corp,dc=com")
    private String userDnPattern;
    @ApiModelProperty(value = "AD 风格 UPN 模板", example = "{0}@corp.com")
    private String upnPattern;
    @ApiModelProperty(value = "查询账号（搜索域用户用）", example = "admin@corp.com")
    private String bindUser;
    @ApiModelProperty(value = "查询账号密码是否已设置（密码本身永不回显）", example = "true")
    private Boolean bindPwdSet;
    /**
     * url + baseDn 齐备即视为已配置
     */
    @ApiModelProperty(value = "url + baseDn 齐备即视为已配置", example = "true")
    private Boolean configured;
    /**
     * true = 来自界面配置；false = 仍是 .env 默认值
     */
    @ApiModelProperty(value = "true 来自界面保存；false 仍是环境变量兜底值", example = "true")
    private Boolean dbConfigured;
}
