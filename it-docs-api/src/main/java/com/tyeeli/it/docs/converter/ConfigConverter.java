package com.tyeeli.it.docs.converter;

import com.tyeeli.it.docs.dto.LdapConfigDTO;
import com.tyeeli.it.docs.service.SysConfigService.LdapConfig;

/**
 * 系统配置 内部对象 → DTO
 */
public final class ConfigConverter {

    private ConfigConverter() {
    }

    public static LdapConfigDTO toDTO(LdapConfig c) {
        if (c == null) {
            return null;
        }
        LdapConfigDTO v = new LdapConfigDTO();
        v.setUrl(c.url);
        v.setBaseDn(c.baseDn);
        v.setUserDnPattern(c.userDnPattern);
        v.setUpnPattern(c.upnPattern);
        v.setBindUser(c.bindUser);
        v.setBindPwdSet(c.bindPwdSet);
        v.setConfigured(c.configured);
        v.setDbConfigured(c.dbConfigured);
        return v;
    }
}
