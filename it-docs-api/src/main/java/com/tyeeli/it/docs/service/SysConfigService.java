package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.dto.LdapConfigRequest;

/**
 * 系统配置（it_config 表）：界面可改的运行时配置。
 *
 * <p>优先级：数据库 &gt; .env 环境变量默认值；保存后立即生效，无需重启。
 * 一期只存 LDAP 四项，键名规范 ldap.url / ldap.base-dn / ldap.user-dn-pattern / ldap.upn-pattern。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.SysConfigServiceImpl}</p>
 */
public interface SysConfigService {

    String KEY_URL = "ldap.url";
    String KEY_BASE_DN = "ldap.base-dn";
    String KEY_USER_DN = "ldap.user-dn-pattern";
    String KEY_UPN = "ldap.upn-pattern";
    String KEY_BIND_USER = "ldap.bind-user";
    String KEY_BIND_PASSWORD = "ldap.bind-password";

    /**
     * 读取生效中的 LDAP 配置（DB 优先，缺失回落 .env）。
     */
    LdapConfig getLdapConfig();

    /**
     * 保存 LDAP 配置（全量覆盖 DB 键，空串视为清空回落默认值），并留审计。
     */
    LdapConfig saveLdapConfig(LdapConfigRequest req, String operator);

    /**
     * LDAP 连接配置（界面配置后的生效值）。
     */
    class LdapConfig {
        public String url;
        public String baseDn;
        public String userDnPattern;
        public String upnPattern;
        /**
         * 查询账号（UPN 或完整 DN）：供「从 AD 导入」搜索域用户，登录 bind 不用它
         */
        public String bindUser;
        /**
         * 查询账号密码（只在保存时提交，任何读取接口都不回显）
         */
        public String bindPassword;
        /**
         * url + baseDn 齐备即视为已配置（登录才会走 LDAP 通道）
         */
        public boolean configured;
        /**
         * 是否来自界面（DB），false = 仍是 .env 默认值
         */
        public boolean dbConfigured;
        /**
         * 查询账号密码是否已配置（界面显示「已设置」标记用）
         */
        public boolean bindPwdSet;
    }
}
