package com.tyeeli.it.docs.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.repository.UserRepository;
import com.tyeeli.it.docs.repository.entity.User;
import com.tyeeli.it.docs.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * 用户数据访问。it_user 同时承载域账号（LDAP）与本地账号，用户名全局唯一。
 *
 * <p>MyBatis-Plus 实现：直接委托 UserMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper mapper;

    /**
     * 按登录名精确查询。登录判定、首登建档、重名校验都走这里，不做模糊匹配。
     */
    @Override
    public User findByUsername(String username) {
        return mapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    /**
     * 按主键查询，不判断状态（状态校验属于业务规则，留给 Service）。
     */
    @Override
    public User findById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 新增用户。主键自增，回填到入参对象上。
     */
    @Override
    public void insert(User user) {
        mapper.insert(user);
    }

    /**
     * 按主键全字段更新，调用方负责先查后改。
     */
    @Override
    public void update(User user) {
        mapper.updateById(user);
    }

    /**
     * 分页查询。keyword 同时模糊匹配 username 与 display_name；authSource 精确过滤 LOCAL/LDAP。按创建时间倒序。
     */
    @Override
    public Page<User> page(String keyword, String authSource, int pageNo, int pageSize) {
        QueryWrapper<User> qw = new QueryWrapper<User>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like("username", keyword.trim()).or().like("display_name", keyword.trim()));
        }
        if (StrUtil.isNotBlank(authSource)) {
            qw.eq("auth_source", authSource.trim());
        }
        qw.orderByDesc("created_at");
        return mapper.selectPage(new Page<User>(pageNo, pageSize), qw);
    }

    /**
     * 登录成功：只更新最后登录时间，不整行覆盖
     */
    @Override
    public void touchLoginSuccess(String username, Date now) {
        UpdateWrapper<User> uw = new UpdateWrapper<User>();
        uw.eq("username", username).set("last_login_at", now);
        mapper.update(null, uw);
    }
}
