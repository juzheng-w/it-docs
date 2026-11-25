package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.DocAcl;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocAclMapper extends BaseMapper<DocAcl> {
}
