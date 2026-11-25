package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.dto.UserCreateRequest;
import com.tyeeli.it.docs.dto.UserPatchRequest;
import com.tyeeli.it.docs.dto.UserQuery;
import com.tyeeli.it.docs.repository.entity.User;

/**
 * 用户管理：查询、ADMIN 建/停用/重置本地账号、自助改密。
 *
 * <p>域账号首次登录自动建档（auth_source=LDAP），本地账号由 ADMIN 创建并强制首登改密。
 * 用户状态、令牌版本由拦截器每次请求以库为准，停用与改密即时生效。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.UserServiceImpl}</p>
 */
public interface UserService {

    /**
     * 按用户名查询，不存在返回 null（登录链路用，不抛异常）。
     */
    User findByUsername(String username);

    /**
     * 按用户名查询并要求状态为 ACTIVE，否则抛业务异常。
     */
    User requireActive(String username);

    /**
     * 用户分页列表，返回前已抹掉密码哈希。
     */
    PageResult<User> list(UserQuery query);

    /**
     * 创建账号（LOCAL 本地 / LDAP 域预建），LOCAL 返回体里带一次性明文初始密码（仅此一次）。
     */
    User create(UserCreateRequest req, String operator);

    /**
     * PATCH 语义：只改提交的字段，null 表示不改。
     */
    void patch(Long id, UserPatchRequest req, String operator);

    /**
     * ADMIN 重置他人密码，返回新的随机密码明文。
     */
    String resetPassword(Long id, String operator);

    /**
     * 用户自助改密：校验旧密码后写入新密码。
     */
    void changePassword(String username, String oldPwd, String newPwd);

    /**
     * 域账号首登建档；已存在则跳过（幂等）。
     */
    void insertIfAbsent(User user);

    /**
     * 登录成功：记录最后登录时间。
     */
    void recordLoginSuccess(String username);
}
