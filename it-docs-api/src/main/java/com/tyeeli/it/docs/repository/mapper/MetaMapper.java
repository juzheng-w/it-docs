package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.Meta;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_meta 字典表映射，纯单表 CRUD，无自定义 SQL。
 */
@Mapper
public interface MetaMapper extends BaseMapper<Meta> {
}
