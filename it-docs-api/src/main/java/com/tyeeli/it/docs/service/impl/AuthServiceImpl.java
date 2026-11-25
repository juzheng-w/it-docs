package com.tyeeli.it.docs.service.impl;

import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.converter.SpaceConverter;
import com.tyeeli.it.docs.dto.ChangePasswordRequest;
import com.tyeeli.it.docs.dto.LoginDTO;
import com.tyeeli.it.docs.dto.LoginRequest;
import com.tyeeli.it.docs.dto.MeDTO;
import com.tyeeli.it.docs.repository.entity.User;
import com.tyeeli.it.docs.service.*;
import com.tyeeli.it.docs.service.LdapService.LdapUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录判定顺序（架构文档 5.2）：
 * 1. 按 username 查 it_user
 * ├─ LOCAL → BCrypt 校验
 * ├─ LDAP  → LDAP bind
 * └─ 未命中 → LDAP bind，成功自动建档（LDAP 用户免注册）
 * 2. 校验通过 → 检查 status=ACTIVE → 签发 JWT
 * LDAP 服务不可用：fail-open=false（默认）→ 仅本地账号可登录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final SpaceService spaceService;
    private final LdapService ldapService;
    private final JwtService jwtService;
    private final OpLogService opLogService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${itdocs.ldap.fail-open:false}")
    private boolean failOpen;

    @Override
    public LoginDTO login(LoginRequest req) {
        String username = req.getUsername() == null ? null : req.getUsername().trim();
        String password = req.getPassword();
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw BizException.badRequest("用户名和密码不能为空");
        }

        User user = userService.findByUsername(username);
        User verified;

        if (user != null && Constants.AUTH_LOCAL.equals(user.getAuthSource())) {
            // 本地账号：BCrypt 校验；LDAP bind 失败且存在同名 LOCAL → 拒绝（不让人绕域密码）
            if (user.getPasswordHash() == null || !encoder.matches(password, user.getPasswordHash())) {
                throw BizException.badRequest("用户名或密码不正确");
            }
            verified = user;
        } else {
            // LDAP 通道（库中无档案 或 LDAP 档案；配置来自管理界面 it_config，.env 兜底）
            LdapUser lu = null;
            try {
                lu = ldapService.bind(username, password);
            } catch (IllegalStateException e) {
                // LDAP 服务不可用
                if (user != null && Constants.AUTH_LDAP.equals(user.getAuthSource()) && !failOpen) {
                    throw BizException.badRequest("域认证服务暂不可用，请稍后重试；应急请用本地管理员账号");
                }
                lu = null; // fail-open 或无档案：允许走本地兜底逻辑（下方再判）
            }
            if (lu != null) {
                verified = (user != null) ? user : autoProvision(username);
            } else if (user != null) {
                // LDAP 密码错 & 库里有档案：LDAP 用户密码错就是错
                throw BizException.badRequest("用户名或密码不正确");
            } else {
                throw BizException.badRequest("用户名或密码不正确");
            }
        }

        // 状态校验（停用即时拒绝，即使 JWT 未过期）
        userService.requireActive(verified.getUsername());

        userService.recordLoginSuccess(verified.getUsername());
        String token = jwtService.issue(verified.getId(), verified.getUsername(),
                verified.getRole(), verified.getDisplayName(),
                verified.getTokenVersion() == null ? 0 : verified.getTokenVersion());
        opLogService.record("LOGIN", null, verified.getUsername(), null, null);

        LoginDTO dto = new LoginDTO();
        dto.setToken(token);
        dto.setUsername(verified.getUsername());
        dto.setDisplayName(verified.getDisplayName());
        dto.setRole(verified.getRole());
        dto.setMustChangePwd(verified.getMustChangePwd() != null && verified.getMustChangePwd() == 1);
        return dto;
    }

    /**
     * 当前用户信息（GET /api/users/me）：个人区域（首访自动创建）+ 可见公共空间列表。
     * 空间按可见性过滤：PUBLIC + INTERNAL 直通，RESTRICTED 需 ACL 命中（ADMIN 直通）。
     * 前端登录后拉一次，个人区域 id 由这里给出。
     */
    @Override
    public MeDTO me(String username) {
        User u = userService.requireActive(username);
        if (u == null) {
            throw BizException.unauthorized("请先登录");
        }
        MeDTO dto = new MeDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setDisplayName(u.getDisplayName());
        dto.setRole(u.getRole());
        dto.setAuthSource(u.getAuthSource());
        dto.setPersonalSpace(SpaceConverter.toDTO(spaceService.ensurePersonalSpace(username)));
        boolean ldapUser = Constants.AUTH_LDAP.equals(u.getAuthSource());
        dto.setSpaces(SpaceConverter.toDTOList(
                spaceService.listVisible(username, u.getRole(), ldapUser)));
        return dto;
    }

    /**
     * 本地账号本人改密（域账号去 AD 改，Service 内会拒）
     */
    @Override
    public void changeLocalPassword(String username, ChangePasswordRequest req) {
        userService.changePassword(username, req.getOldPassword(), req.getNewPassword());
    }

    /**
     * LDAP 用户首登自动建档：默认 VIEWER，ADMIN 只提权不建号
     */
    private User autoProvision(String username) {
        User u = new User();
        u.setUsername(username);
        u.setDisplayName(username);
        u.setAuthSource(Constants.AUTH_LDAP);
        u.setRole(Constants.ROLE_VIEWER);
        u.setStatus(Constants.STATUS_ACTIVE);
        u.setMustChangePwd(0);
        userService.insertIfAbsent(u);
        log.info("LDAP 用户首登自动建档 username={}", username);
        return u;
    }
}
