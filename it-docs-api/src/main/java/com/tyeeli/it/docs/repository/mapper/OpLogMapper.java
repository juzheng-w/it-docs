package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.OpLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_op_log 操作审计表映射，只追加不修改，无自定义 SQL。
 */
@Mapper
public interface OpLogMapper extends BaseMapper<OpLog> {
}
