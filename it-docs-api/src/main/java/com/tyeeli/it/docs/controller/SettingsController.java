package com.tyeeli.it.docs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import cn.hutool.core.util.StrUtil;
import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.AuthContext;
import com.tyeeli.it.docs.config.RequireRole;
import com.tyeeli.it.docs.converter.ConfigConverter;
import com.tyeeli.it.docs.dto.LdapConfigDTO;
import com.tyeeli.it.docs.dto.LdapConfigRequest;
import com.tyeeli.it.docs.dto.LdapTestRequest;
import com.tyeeli.it.docs.dto.MessageDTO;
import com.tyeeli.it.docs.service.LdapService;
import com.tyeeli.it.docs.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 系统设置（仅 ADMIN）：
 * GET  /api/settings/ldap       读当前生效的 LDAP 配置
 * PUT  /api/settings/ldap       保存（即生效，无需重启）
 * POST /api/settings/ldap/test  测试连接（配置按提交值试，通过再保存）
 */
@Api(tags = "07. 系统设置", description = "LDAP 域账号接入配置，界面保存即生效（落 it_config 表），无需改配置文件或重启")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@RequireRole(Constants.ROLE_ADMIN)
public class SettingsController {

    private final SysConfigService sysConfigService;
    private final LdapService ldapService;

    @ApiOperation(value = "读取 LDAP 配置",
            notes = "返回当前生效值（不含任何密码）。dbConfigured=true 表示来自界面保存，"
                    + "false 表示仍是环境变量/配置文件里的兜底默认值。\n\n"
                    + "失败：403 非管理员。")
    @GetMapping("/ldap")
    public Result<LdapConfigDTO> getLdap() {
        return Result.ok(ConfigConverter.toDTO(sysConfigService.getLdapConfig()));
    }

    @ApiOperation(value = "保存 LDAP 配置",
            notes = "保存后立即生效，无需重启。url 或 baseDn 传空串表示清空该键并回落到默认值。"
                    + "两种命名风格二选一：OpenLDAP 用 userDnPattern（uid={0},ou=people），AD 用 upnPattern（{0}@corp.com）。\n\n"
                    + "失败：400 配置值不合法；403 非管理员。")
    @PutMapping("/ldap")
    public Result<LdapConfigDTO> saveLdap(@Valid @RequestBody LdapConfigRequest req) {
        SysConfigService.LdapConfig saved =
                sysConfigService.saveLdapConfig(req, AuthContext.require().username);
        return Result.ok(ConfigConverter.toDTO(saved));
    }

    /**
     * 填了 testUser/testPwd 走真实 bind；没填只探测服务器连通性
     */
    @ApiOperation(value = "测试 LDAP 连接",
            notes = "按请求体里的配置试连，不落库。填了 testUser/testPwd 会做真实 bind 校验；"
                    + "不填只探测地址可达与 Base DN 是否有效。建议「先测试通过再保存」。\n\n"
                    + "失败：400 url 或 baseDn 缺失。")
    @PostMapping("/ldap/test")
    public Result<MessageDTO> testLdap(@RequestBody LdapTestRequest req) {
        SysConfigService.LdapConfig cfg = new SysConfigService.LdapConfig();
        cfg.url = req.getUrl();
        cfg.baseDn = req.getBaseDn();
        cfg.userDnPattern = req.getUserDnPattern();
        cfg.upnPattern = req.getUpnPattern();
        cfg.configured = StrUtil.isNotBlank(cfg.url) && StrUtil.isNotBlank(cfg.baseDn);
        if (!cfg.configured) {
            throw BizException.badRequest("请先填写 LDAP 地址和 Base DN");
        }
        return Result.ok(new MessageDTO(ldapService.test(cfg, req.getTestUser(), req.getTestPwd())));
    }
}
