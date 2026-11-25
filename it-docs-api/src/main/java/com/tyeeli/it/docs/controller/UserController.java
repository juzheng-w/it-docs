package com.tyeeli.it.docs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.AuthContext;
import com.tyeeli.it.docs.config.RequireRole;
import com.tyeeli.it.docs.converter.UserConverter;
import com.tyeeli.it.docs.dto.*;
import com.tyeeli.it.docs.repository.entity.User;
import com.tyeeli.it.docs.service.AuthService;
import com.tyeeli.it.docs.service.LdapService;
import com.tyeeli.it.docs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户资源。
 * /me 是「当前登录用户」子资源（任何已登录角色可访问）；
 * 其余成员管理仅 ADMIN，权限由 @RequireRole 在拦截器统一判定。
 */
@Api(tags = "05. 用户与成员", description = "/me 为当前登录用户自助接口；成员列表、建号、改角色状态、重置密码限 ADMIN")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final LdapService ldapService;

    // ---------------- 当前用户（/me） ----------------

    /**
     * 当前用户 + 个人区域 + 公共空间（前端登录后拉一次）
     */
    @ApiOperation(value = "当前登录用户",
            notes = "登录后前端拉一次：拿身份信息、个人区域（首次访问自动创建）与可读公共空间列表。\n\n"
                    + "失败：401 未登录或 token 失效。")
    @GetMapping("/me")
    @RequireRole({Constants.ROLE_ADMIN, Constants.ROLE_EDITOR, Constants.ROLE_VIEWER})
    public Result<MeDTO> me() {
        return Result.ok(authService.me(AuthContext.require().username));
    }

    /**
     * 本人改密（本地账号；域账号去 AD 改）
     */
    @ApiOperation(value = "修改本人密码",
            notes = "仅本地账号有效，域账号请到 AD 修改。新密码至少 8 位且同时包含字母与数字。\n\n"
                    + "失败：400 原密码错误或新密码不合规。")
    @PutMapping("/me/password")
    @RequireRole({Constants.ROLE_ADMIN, Constants.ROLE_EDITOR, Constants.ROLE_VIEWER})
    public Result<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changeLocalPassword(AuthContext.require().username, req);
        return Result.ok();
    }

    // ---------------- 成员管理（ADMIN） ----------------

    @ApiOperation(value = "成员列表",
            notes = "支持按用户名/姓名模糊（keyword）与账号来源（authSource=LOCAL/LDAP）过滤，分页默认 20、上限 100。\n\n"
                    + "失败：403 非管理员。")
    @GetMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<PageResult<UserDTO>> list( UserQuery query) {
        PageResult<User> page = userService.list(query);
        List<UserDTO> list = UserConverter.toDTOList(page.getList());
        return Result.ok(PageResult.of(page.getTotal(), page.getPage(), page.getSize(), list));
    }

    /**
     * 创建本地账号；响应里带一次性临时密码
     */
    @ApiOperation(value = "创建账号",
            notes = "authSource=LOCAL（默认）：临时密码 + 首登改密，tempPassword 只在此刻返回一次；"
                    + "authSource=LDAP：预建域档案（不设密码、首登不须改密），用户直接用 AD 密码登录。\n\n"
                    + "失败：400 用户名不合规或已存在；403 非管理员。")
    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<UserDTO> create(@Valid @RequestBody UserCreateRequest req) {
        User created = userService.create(req, AuthContext.require().username);
        return Result.ok(UserConverter.toDTO(created));
    }

    /**
     * 局部更新：改角色 / 停用启用 / 改姓名邮箱，只提交要改的字段
     */
    @ApiOperation(value = "局部更新成员",
            notes = "PATCH 语义：只提交要改的字段，null 表示保持不变。可改 role（ADMIN/EDITOR/VIEWER）、"
                    + "status（ACTIVE/DISABLED）、displayName、email。停用后立即失去访问权。\n\n"
                    + "失败：403 非管理员；404 用户不存在。")
    @PatchMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> patch(
            @ApiParam(value = "用户 ID", required = true, example = "2") @PathVariable Long id,
            @Valid @RequestBody UserPatchRequest req) {
        userService.patch(id, req, AuthContext.require().username);
        return Result.ok();
    }

    /**
     * 管理员重置本地账号密码，返回一次性临时密码
     */
    @ApiOperation(value = "重置成员密码",
            notes = "重置为随机临时密码并置「首次登录须改密」，明文只在本次响应里出现一次。\n\n"
                    + "失败：403 非管理员；404 用户不存在。")
    @PutMapping("/{id}/password")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<MessageDTO> resetPassword(
            @ApiParam(value = "用户 ID", required = true, example = "2") @PathVariable Long id) {
        String temp = userService.resetPassword(id, AuthContext.require().username);
        return Result.ok(new MessageDTO(temp));
    }

    // ---------------- 从 AD 导入（ADMIN） ----------------

    @ApiOperation(value = "搜索 AD 域用户（分页）",
            notes = "「从 AD 导入」弹窗用：关键词模糊搜索（登录名/姓名/UPN，未禁用）；"
                    + "关键词为空则列出全部可用域用户。分页参数 page/pageSize，exists 标记本库是否已存在。\n\n"
                    + "失败：503 未配置 LDAP 或查询账号。")
    @GetMapping("/ldap")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<PageResult<LdapUserDTO>> searchLdap(
            @ApiParam(value = "搜索关键词，空则列出全部域用户", example = "zhang")
            @RequestParam(required = false) String keyword,
            @ApiParam(value = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") int page,
            @ApiParam(value = "每页条数（1~100）", example = "20") @RequestParam(defaultValue = "20") int pageSize) {
        LdapService.LdapPage p = ldapService.searchUsers(keyword, page, pageSize);
        List<LdapUserDTO> list = new ArrayList<>();
        for (LdapService.LdapUser u : p.items) {
            LdapUserDTO dto = new LdapUserDTO();
            dto.setUsername(u.username);
            dto.setDisplayName(u.displayName);
            dto.setEmail(u.email);
            dto.setExists(userService.findByUsername(u.username) != null);
            list.add(dto);
        }
        return Result.ok(PageResult.of(p.total, page, pageSize, list));
    }

    @ApiOperation(value = "批量导入 AD 域账号",
            notes = "勾选的域用户批量建档（authSource=LDAP，无密码，用户直接用域密码登录），统一授予 role。"
                    + "已存在的自动跳过并在 message 里说明。\n\n"
                    + "失败：403 非管理员。")
    @PostMapping("/import-ldap")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<MessageDTO> importLdap(@Valid @RequestBody LdapImportRequest req) {
        String operator = AuthContext.require().username;
        int ok = 0, skip = 0;
        for (LdapImportRequest.Item item : req.getUsers()) {
            if (userService.findByUsername(item.getUsername()) != null) {
                skip++;
                continue;
            }
            UserCreateRequest one = new UserCreateRequest();
            one.setAuthSource(Constants.AUTH_LDAP);
            one.setUsername(item.getUsername());
            one.setDisplayName(item.getDisplayName());
            one.setEmail(item.getEmail());
            one.setRole(req.getRole());
            one.setSysScope(req.getSysScope());
            userService.create(one, operator);
            ok++;
        }
        String msg = skip == 0
                ? "导入成功 " + ok + " 个域账号，用户可直接用 AD 密码登录"
                : "导入成功 " + ok + " 个，跳过 " + skip + " 个（本库已存在）";
        return Result.ok(new MessageDTO(msg));
    }
}
