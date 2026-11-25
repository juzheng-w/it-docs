package com.tyeeli.it.docs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.dto.LoginDTO;
import com.tyeeli.it.docs.dto.LoginRequest;
import com.tyeeli.it.docs.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口。
 * 登录是「用凭证换 token」，REST 里用 POST 提交凭证是通行做法（不是资源 CRUD，无需硬凑）。
 */
@Api(tags = "01. 认证", description = "登录换取 JWT。除登录外，其余接口都要求 Authorization: Bearer <token>")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录：本地账号 BCrypt 校验，域账号走 LDAP bind，首登自动建档
     */
    @ApiOperation(value = "登录",
            notes = "本地账号走 BCrypt 校验，域账号走 LDAP bind，首次登录自动建档并标记强制改密。\n\n"
                    + "拿到的 token 粘到右上角 Authorize 即可调试后续接口，有效期 12 小时。\n\n"
                    + "失败：400 用户名或密码为空；401 用户名或密码错误 / 账号已停用。")
        @PostMapping("/login")
    public Result<LoginDTO> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }
}
