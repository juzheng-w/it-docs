package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.FileMeta;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_file 文件元数据表映射，纯单表 CRUD，无自定义 SQL。
 */
@Mapper
public interface FileMetaMapper extends BaseMapper<FileMeta> {
}
