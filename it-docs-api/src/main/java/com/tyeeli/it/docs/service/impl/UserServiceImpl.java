package com.tyeeli.it.docs.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.converter.UserConverter;
import com.tyeeli.it.docs.dto.UserCreateRequest;
import com.tyeeli.it.docs.dto.UserPatchRequest;
import com.tyeeli.it.docs.dto.UserQuery;
import com.tyeeli.it.docs.repository.UserRepository;
import com.tyeeli.it.docs.repository.entity.User;
import com.tyeeli.it.docs.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * 用户管理：ADMIN 建/停用/重置本地账号；LDAP 用户首登自动建档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User requireActive(String username) {
        User u = findByUsername(username);
        if (u == null) {
            return null;
        }
        if (Constants.STATUS_DISABLED.equals(u.getStatus())) {
            throw BizException.forbidden("账号已停用，请联系管理员");
        }
        return u;
    }

    @Override
    public PageResult<User> list(UserQuery query) {
        int page = query.pageNo();
        int size = query.pageSize();
        String keyword = query.getKeyword();
        String authSource = query.getAuthSource();
        Page<User> p = userRepository.page(keyword, authSource, page, size);
        p.getRecords().forEach(u -> u.setPasswordHash(null)); // 不回传哈希
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    /**
     * ADMIN 创建账号：
     * - LOCAL：临时密码 + 首登改密；
     * - LDAP：预建域档案，不设密码（用户直接用 AD 密码登录，登录链路见 AuthServiceImpl），
     *   提前建号的意义是可直接指定角色，不用等用户首登自动建档后手动提权。
     * 不得与已有账号重名（防混淆）。
     */
    @Override
    public User create(UserCreateRequest req, String operator) {
        String username = req.getUsername();
        if (username == null || !username.matches("[a-zA-Z0-9_.-]{3,50}")) {
            throw BizException.badRequest("用户名 3~50 位，仅字母数字._-");
        }
        if (findByUsername(username) != null) {
            throw BizException.badRequest("用户名已存在（含域账号），不能重复");
        }
        String displayName = req.getDisplayName();
        String role = req.getRole();
        String email = req.getEmail();
        boolean ldap = Constants.AUTH_LDAP.equals(req.getAuthSource());
        String tempPwd = ldap ? null : "Itd@" + UUID.randomUUID().toString().substring(0, 8);
        User u = new User();
        u.setUsername(username);
        u.setDisplayName(displayName == null ? username : displayName);
        u.setAuthSource(ldap ? Constants.AUTH_LDAP : Constants.AUTH_LOCAL);
        u.setPasswordHash(ldap ? null : encoder.encode(tempPwd));
        u.setRole(role == null ? Constants.ROLE_VIEWER : role);
        u.setSysScope(UserConverter.joinScope(req.getSysScope()));
        u.setEmail(email);
        u.setStatus(Constants.STATUS_ACTIVE);
        u.setMustChangePwd(ldap ? 0 : 1);
        u.setCreatedBy(operator);
        userRepository.insert(u);
        log.info("账号创建 authSource={} username={} operator={}", u.getAuthSource(), username, operator);
        u.setPasswordHash(null);
        u.setTempPassword(tempPwd); // 只在创建响应里出现一次（LDAP 为 null）
        return u;
    }

    /**
     * PATCH 语义：只改提交的字段（null 表示不改）；角色/停用即时生效（拦截器每次以库为准）
     */
    @Override
    public void patch(Long id, UserPatchRequest req, String operator) {
        User u = mustGet(id);
        String oldRole = u.getRole();
        String oldStatus = u.getStatus();
        if (req.getRole() != null && !req.getRole().equals(oldRole)) {
            u.setRole(req.getRole());
        }
        if (req.getStatus() != null && !req.getStatus().equals(oldStatus)) {
            u.setStatus(req.getStatus());
        }
        if (req.getDisplayName() != null && !req.getDisplayName().trim().isEmpty()) {
            u.setDisplayName(req.getDisplayName().trim());
        }
        if (req.getEmail() != null) {
            u.setEmail(req.getEmail().trim());
        }
        if (req.getSysScope() != null) {
            // null=不修改；空数组=清空（恢复全部系统）
            u.setSysScope(UserConverter.joinScope(req.getSysScope()));
        }
        u.setUpdatedAt(new Date());
        userRepository.update(u);
        log.info("用户更新 username={} role {}->{} status {}->{} operator={}",
                u.getUsername(), oldRole, u.getRole(), oldStatus, u.getStatus(), operator);
    }

    @Override
    public String resetPassword(Long id, String operator) {
        User u = mustGet(id);
        if (!Constants.AUTH_LOCAL.equals(u.getAuthSource())) {
            throw BizException.badRequest("域账号密码在 AD 域修改，平台不能重置");
        }
        String tempPwd = "Itd@" + UUID.randomUUID().toString().substring(0, 8);
        u.setPasswordHash(encoder.encode(tempPwd));
        u.setMustChangePwd(1);
        bumpTokenVersion(u);
        u.setUpdatedAt(new Date());
        userRepository.update(u);
        log.info("密码重置 username={} operator={}", u.getUsername(), operator);
        return tempPwd;
    }

    /**
     * 本人改密（本地账号 / 首登强制改密共用）
     */
    @Override
    public void changePassword(String username, String oldPwd, String newPwd) {
        User u = findByUsername(username);
        if (u == null || !Constants.AUTH_LOCAL.equals(u.getAuthSource())) {
            throw BizException.badRequest("域账号密码在 AD 域修改");
        }
        if (!encoder.matches(oldPwd, u.getPasswordHash())) {
            throw BizException.badRequest("原密码不正确");
        }
        if (newPwd == null || newPwd.length() < 8 || !newPwd.matches(".*[a-zA-Z].*") || !newPwd.matches(".*\\d.*")) {
            throw BizException.badRequest("新密码至少 8 位且含字母和数字");
        }
        u.setPasswordHash(encoder.encode(newPwd));
        u.setMustChangePwd(0);
        bumpTokenVersion(u);
        u.setUpdatedAt(new Date());
        userRepository.update(u);
    }

    /**
     * 令牌版本自增：旧 JWT 的 tv 不再匹配，拦截器直接判失效
     */
    private void bumpTokenVersion(User u) {
        u.setTokenVersion((u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1);
    }

    @Override
    public void recordLoginSuccess(String username) {
        userRepository.touchLoginSuccess(username, new Date());
    }

    private User mustGet(Long id) {
        User u = userRepository.findById(id);
        if (u == null) {
            throw BizException.notFound("用户不存在");
        }
        return u;
    }

    /**
     * LDAP 首登自动建档：并发下防重复（username 唯一键兜底）
     */
    @Override
    public void insertIfAbsent(User u) {
        try {
            userRepository.insert(u);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("自动建档撞唯一键（并发登录）username={}", u.getUsername());
        }
    }
}
