package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.dto.ChangePasswordRequest;
import com.tyeeli.it.docs.dto.LoginDTO;
import com.tyeeli.it.docs.dto.LoginRequest;
import com.tyeeli.it.docs.dto.MeDTO;

/**
 * 认证服务：登录、当前用户信息、修改本地密码。
 *
 * <p>登录判定顺序（架构文档 5.2）：
 * 1. 按 username 查 it_user
 * - LOCAL  → BCrypt 校验
 * - LDAP   → LDAP bind
 * - 未命中 → LDAP bind，成功自动建档（域用户免注册）
 * 2. 校验通过 → 检查 status=ACTIVE → 签发 JWT。
 * LDAP 不可用时 fail-open=false（默认）：只允许本地账号登录。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.AuthServiceImpl}</p>
 */
public interface AuthService {

    /**
     * 账号密码登录，成功返回 JWT 与用户基本信息。
     *
     * @param req 用户名 + 密码
     * @return token、角色、是否需强制改密
     * @throws com.tyeeli.it.docs.common.BizException 账号停用/密码错误/域认证失败
     */
    LoginDTO login(LoginRequest req);

    /**
     * 当前登录用户详情，附带个人区域 id（首次访问按需创建）。
     */
    MeDTO me(String username);

    /**
     * 修改自己的本地账号密码；域账号不允许改（密码在 AD 侧）。
     */
    void changeLocalPassword(String username, ChangePasswordRequest req);
}
