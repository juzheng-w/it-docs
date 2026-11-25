package com.tyeeli.it.docs.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyeeli.it.docs.repository.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * it_user 用户表映射，纯单表 CRUD，无自定义 SQL。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
