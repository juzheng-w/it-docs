package com.tyeeli.it.docs.service.impl;

import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 签发与解析（jjwt 0.9.1）。
 * 载荷：uid / username / role / displayName / tv；有效期默认 12h。
 * 密钥从环境变量注入，不入库不入 git。
 */
@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${itdocs.jwt.secret:itdocs-default-secret-change-me}")
    private String secret;

    @Value("${itdocs.jwt.expire-hours:12}")
    private long expireHours;

    @Override
    public String issue(Long uid, String username, String role, String displayName, int tokenVersion) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("uid", uid);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("displayName", displayName == null ? username : displayName);
        claims.put("tv", tokenVersion);
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireHours * 3600_000L))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * 解析失败（过期/伪造）统一 401
     */
    @Override
    public Claims parse(String token) {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            throw BizException.unauthorized("登录已过期，请重新登录");
        }
    }
}
