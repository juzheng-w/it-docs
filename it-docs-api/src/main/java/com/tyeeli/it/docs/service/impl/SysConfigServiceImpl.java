package com.tyeeli.it.docs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.tyeeli.it.docs.dto.LdapConfigRequest;
import com.tyeeli.it.docs.repository.ConfigRepository;
import com.tyeeli.it.docs.repository.entity.Config;
import com.tyeeli.it.docs.service.OpLogService;
import com.tyeeli.it.docs.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统配置（it_config 表）：界面可改的运行时配置。
 * 优先级：数据库 > .env 环境变量默认值。保存后即生效，无需重启。
 * 一期只存 LDAP 四项；键名规范：ldap.url / ldap.base-dn / ldap.user-dn-pattern / ldap.upn-pattern。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    public static final String KEY_URL = "ldap.url";
    public static final String KEY_BASE_DN = "ldap.base-dn";
    public static final String KEY_USER_DN = "ldap.user-dn-pattern";
    public static final String KEY_UPN = "ldap.upn-pattern";
    private final ConfigRepository configRepository;
    private final OpLogService opLogService;
    @Value("${itdocs.ldap.url:}")
    private String envUrl;
    @Value("${itdocs.ldap.base-dn:}")
    private String envBaseDn;
    @Value("${itdocs.ldap.user-dn-pattern:}")
    private String envUserDnPattern;
    @Value("${itdocs.ldap.upn-pattern:}")
    private String envUpnPattern;
    @Value("${itdocs.ldap.bind-user:}")
    private String envBindUser;
    @Value("${itdocs.ldap.bind-password:}")
    private String envBindPassword;

    /**
     * LDAP 配置（DB 优先，DB 无该键则回落 .env 默认值；bindPassword 永不通过读取接口回显）
     */
    @Override
    public LdapConfig getLdapConfig() {
        LdapConfig c = new LdapConfig();
        c.url = withDb(KEY_URL, envUrl);
        c.baseDn = withDb(KEY_BASE_DN, envBaseDn);
        c.userDnPattern = withDb(KEY_USER_DN, envUserDnPattern);
        c.upnPattern = withDb(KEY_UPN, envUpnPattern);
        c.bindUser = withDb(KEY_BIND_USER, envBindUser);
        c.bindPassword = withDb(KEY_BIND_PASSWORD, envBindPassword);
        c.configured = StrUtil.isNotBlank(c.url) && StrUtil.isNotBlank(c.baseDn);
        c.dbConfigured = configRepository.findById(KEY_URL) != null;
        c.bindPwdSet = StrUtil.isNotBlank(c.bindPassword);
        return c;
    }

    /**
     * 管理界面保存 LDAP 配置（全量覆盖 DB 键；空串视为清空该项回落默认值）。
     * bindPassword 例外：空串/null 表示「保持不变」，只有提交非空值才更新——避免读取接口不回显导致保存时被清掉。
     * 多行 upsert 同事务，避免只写进去一半导致 LDAP 配置前后不一致。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LdapConfig saveLdapConfig(LdapConfigRequest req, String operator) {
        Map<String, String> before = ldapSnapshot();
        upsert(KEY_URL, req.getUrl());
        upsert(KEY_BASE_DN, req.getBaseDn());
        upsert(KEY_USER_DN, req.getUserDnPattern());
        upsert(KEY_UPN, req.getUpnPattern());
        upsert(KEY_BIND_USER, req.getBindUser());
        if (StrUtil.isNotBlank(req.getBindPassword())) {
            upsert(KEY_BIND_PASSWORD, req.getBindPassword());
        }
        opLogService.record("UPDATE", null, operator,
                json(before), json(ldapSnapshot()));
        log.info("LDAP 配置已通过界面更新 operator={}", operator);
        return getLdapConfig();
    }

    // ---------------- internal ----------------

    private String withDb(String key, String envDefault) {
        Config row = configRepository.findById(key);
        if (row != null && row.getCfgValue() != null) {
            return row.getCfgValue();
        }
        return envDefault;
    }

    private void upsert(String key, String value) {
        if (value == null) {
            value = "";
        }
        Config row = configRepository.findById(key);
        if (row == null) {
            row = new Config();
            row.setCfgKey(key);
            row.setCfgValue(value);
            row.setRemark("LDAP 界面配置");
            configRepository.insert(row);
        } else {
            row.setCfgValue(value);
            configRepository.update(row);
        }
    }

    private Map<String, String> ldapSnapshot() {
        LdapConfig c = getLdapConfig();
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("url", c.url);
        m.put("baseDn", c.baseDn);
        m.put("userDnPattern", c.userDnPattern);
        m.put("upnPattern", c.upnPattern);
        return m;
    }

    private String json(Map<String, String> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":\"")
                    .append(e.getValue() == null ? "" : e.getValue()).append("\"");
        }
        return sb.append("}").toString();
    }

}
