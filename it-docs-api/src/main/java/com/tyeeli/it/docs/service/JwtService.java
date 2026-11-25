package com.tyeeli.it.docs.service;

import io.jsonwebtoken.Claims;

/**
 * JWT 签发与解析（jjwt 0.9.1）。
 *
 * <p>载荷：uid / username / role / displayName / tv（令牌版本）；有效期默认 12 小时。
 * 密钥从环境变量注入，不入库、不入 git。角色只作参考，鉴权以库中最新值为准；
 * tv 与 it_user.token_version 不一致时该 token 视为失效（改密、重置后立即生效）。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.JwtServiceImpl}</p>
 */
public interface JwtService {

    /**
     * 签发 token。
     */
    String issue(Long uid, String username, String role, String displayName, int tokenVersion);

    /**
     * 解析并校验 token，签名过期或不合法时抛运行时异常，由拦截器转 401。
     */
    Claims parse(String token);
}
