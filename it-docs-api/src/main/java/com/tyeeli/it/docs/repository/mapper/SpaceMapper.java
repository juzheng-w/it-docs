package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.Space;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_space 空间表映射，纯单表 CRUD，无自定义 SQL。
 */
@Mapper
public interface SpaceMapper extends BaseMapper<Space> {
}
