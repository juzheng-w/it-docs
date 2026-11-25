package com.tyeeli.it.docs.repository.impl;

import com.tyeeli.it.docs.repository.ConfigRepository;
import com.tyeeli.it.docs.repository.entity.Config;
import com.tyeeli.it.docs.repository.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 系统配置数据访问（it_config 键值表）。数据库值优先于 .env 默认值。
 *
 * <p>MyBatis-Plus 实现：直接委托 ConfigMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class ConfigRepositoryImpl implements ConfigRepository {

    private final ConfigMapper mapper;

    /**
     * 按配置键取值；键名形如 ldap.url，主键即键名。
     */
    @Override
    public Config findById(String key) {
        return mapper.selectById(key);
    }

    /**
     * 新增配置项。
     */
    @Override
    public void insert(Config config) {
        mapper.insert(config);
    }

    /**
     * 按主键更新配置值。
     */
    @Override
    public void update(Config config) {
        mapper.updateById(config);
    }
}
