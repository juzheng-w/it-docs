package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.Doc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * it_doc 文档表映射。
 * 单表 CRUD 走 MyBatis-Plus，只有空间计数需要手写 SQL；全文检索由带过滤条件的 QueryWrapper 执行。
 */
@Mapper
public interface DocMapper extends BaseMapper<Doc> {

    /**
     * 空间文档计数（个人区域页脚统计用）
     */
    @Select("SELECT COUNT(*) FROM it_doc WHERE space_id = #{spaceId} AND status != 'DELETED'")
    int countBySpace(@Param("spaceId") Long spaceId);
}
