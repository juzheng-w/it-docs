package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.GroupBind;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_group_bind 钉钉群空间绑定表映射（二期），一期只建表。
 */
@Mapper
public interface GroupBindMapper extends BaseMapper<GroupBind> {
}
