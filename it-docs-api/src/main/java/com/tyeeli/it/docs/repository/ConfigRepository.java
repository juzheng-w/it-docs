package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.Config;

/**
 * 系统配置数据访问（it_config 键值表）。数据库值优先于 .env 默认值。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface ConfigRepository {
    /**
     * 按配置键取值；键名形如 ldap.url，主键即键名。
     */
    Config findById(String key);

    /**
     * 新增配置项。
     */
    void insert(Config config);

    /**
     * 按主键更新配置值。
     */
    void update(Config config);
}
