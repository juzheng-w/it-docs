package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.DocVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_doc_version 文档版本快照表映射，纯单表 CRUD，无自定义 SQL。
 */
@Mapper
public interface DocVersionMapper extends BaseMapper<DocVersion> {
}
