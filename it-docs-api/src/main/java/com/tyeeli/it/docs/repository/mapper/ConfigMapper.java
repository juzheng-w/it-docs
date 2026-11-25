package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.Config;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_config 键值配置表映射，纯单表 CRUD，无自定义 SQL。
 */
@Mapper
public interface ConfigMapper extends BaseMapper<Config> {
}
