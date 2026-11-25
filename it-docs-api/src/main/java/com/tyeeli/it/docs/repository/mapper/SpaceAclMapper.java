package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.SpaceAcl;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_space_acl 空间授权表映射，纯单表 CRUD，无自定义 SQL。
 */
@Mapper
public interface SpaceAclMapper extends BaseMapper<SpaceAcl> {
}
