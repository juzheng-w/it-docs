package com.tyeeli.it.docs.config;

import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.repository.entity.User;
import com.tyeeli.it.docs.service.JwtService;
import com.tyeeli.it.docs.service.UserService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 统一认证 + 鉴权拦截器（MVC 的 Web 层横切）：
 * 1. 认证：白名单放行（公开查阅）；其余必须带 JWT，解析后校验用户状态、令牌版本、强制改密
 * 2. 鉴权：读 @RequireRole（方法优先、类其次），角色不符 403
 * 3. 上下文：登录用户挂 ThreadLocal，请求结束清理
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final UserService userService;
    /**
     * 编译后的白名单（见 compilePublicPaths）
     */
    private final java.util.List<Rule> rules = new java.util.ArrayList<Rule>();
    @Value("${itdocs.auth.public-paths:POST:/api/auth/login,GET:/api/docs,GET:/api/docs/**,GET:/api/spaces,GET:/api/spaces/**,GET:/api/meta,GET:/api/files/*/view,GET:/api/files/*/download,/error}")
    private String publicPaths;

    public JwtInterceptor(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * = 单段，** = 多段，其余按字面量；整体完全匹配（包级可见，便于单测）
     */
    static String toRegex(String pattern) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                    sb.append(".*");
                    i++;
                } else {
                    sb.append("[^/]*");
                }
            } else if ("\\.[]{}()+-^$|?".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.append('$').toString();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri.equals("/api/docs") || uri.startsWith("/api/docs/")
                || uri.startsWith("/api/files/")) {
            // 文档内容和附件可能按用户/单篇 ACL 收紧，避免浏览器或共享缓存保留已撤权内容。
            response.setHeader("Cache-Control", "private, no-store");
        }
        if (isPublic(request.getMethod(), uri)) {
            // 公开路径：带 token 也解析（个人空间列表、图片预览依赖当前用户），不带也放行
            tryBindUser(request);
        } else {
            if (!authenticate(request, response)) {
                return false;
            }
        }
        return checkRole(handler, response);
    }

    // ---------------- 认证 ----------------

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = extractToken(request);
        if (token == null) {
            writeJson(response, 401, "请先登录");
            return false;
        }
        String username;
        Number uid;
        Number tv;
        try {
            Claims claims = jwtService.parse(token);
            username = (String) claims.get("username");
            uid = (Number) claims.get("uid");
            tv = (Number) claims.get("tv");
        } catch (Exception e) {
            writeJson(response, 401, "登录已失效，请重新登录");
            return false;
        }

        // 每次请求以库为准：状态、角色、令牌版本都取最新值，停用/提权/改密即时生效
        User u = userService.findByUsername(username);
        if (u == null || !Constants.STATUS_ACTIVE.equals(u.getStatus())) {
            writeJson(response, 401, "账号已停用或不存在");
            return false;
        }
        if (tokenVersionOf(tv) != tokenVersionOf(u)) {
            writeJson(response, 401, "登录已失效，请重新登录");
            return false;
        }
        if (mustChangePwd(u) && !isPasswordChangeRequest(request)) {
            writeJson(response, 403, "请先修改初始密码");
            return false;
        }
        AuthContext.set(new AuthContext.LoginUser(uid == null ? null : uid.longValue(), username,
                u.getDisplayName(), u.getRole(), Constants.AUTH_LDAP.equals(u.getAuthSource())));
        return true;
    }

    // ---------------- 鉴权 ----------------

    /**
     * 公开路径带 token 时尽力绑定身份，坏 token 按未登录处理，不拦请求
     */
    private void tryBindUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return;
        }
        try {
            Claims claims = jwtService.parse(token);
            String username = (String) claims.get("username");
            Number tv = (Number) claims.get("tv");
            User u = userService.findByUsername(username);
            if (u == null || !Constants.STATUS_ACTIVE.equals(u.getStatus())) {
                return;
            }
            if (tokenVersionOf(tv) != tokenVersionOf(u) || mustChangePwd(u)) {
                return;
            }
            AuthContext.set(new AuthContext.LoginUser(u.getId(), username,
                    u.getDisplayName(), u.getRole(), Constants.AUTH_LDAP.equals(u.getAuthSource())));
        } catch (Exception ignore) {
            // 公开路径带坏 token 不拦，按未登录处理
        }
    }

    /**
     * 未带 tv 的旧 token 按 0 处理，与库中默认值一致
     */
    private static int tokenVersionOf(Object tv) {
        return tv instanceof Number ? ((Number) tv).intValue() : 0;
    }

    private static int tokenVersionOf(User u) {
        return u.getTokenVersion() == null ? 0 : u.getTokenVersion();
    }

    private static boolean mustChangePwd(User u) {
        return u.getMustChangePwd() != null && u.getMustChangePwd() == 1;
    }

    /**
     * 强制改密期间只放行「查本人信息」与「改密」两个接口，其余业务接口一律 403
     */
    private static boolean isPasswordChangeRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return ("GET".equalsIgnoreCase(method) && "/api/users/me".equals(uri))
                || ("PUT".equalsIgnoreCase(method) && "/api/users/me/password".equals(uri));
    }

    // ---------------- 工具 ----------------

    private boolean checkRole(Object handler, HttpServletResponse response) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        RequireRole ann = hm.getMethodAnnotation(RequireRole.class);
        if (ann == null) {
            ann = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (ann == null || ann.value().length == 0) {
            return true;
        }
        AuthContext.LoginUser u = AuthContext.get();
        if (u == null) {
            writeJson(response, 401, "请先登录");
            return false;
        }
        for (String allowed : ann.value()) {
            if (allowed.equals(u.role)) {
                return true;
            }
        }
        writeJson(response, 403, "权限不足，需要 " + String.join("/", ann.value()) + " 角色");
        return false;
    }

    /**
     * 白名单匹配（启动时编译为正则，逐条整体匹配）：
     * GET:/api/files/&#42;/view  → 单段通配，只放行 view，不会连带放行 download
     * GET:/api/docs/&#42;&#42;       → 多段通配
     * /error                  → 不限方法
     * 注意：不带通配符即完全匹配，不再默认前缀匹配——旧实现 GET:/api/files
     * 会把 /api/files/{id}/download 一起放行，导致个人区附件可被匿名下载。
     */
    private boolean isPublic(String method, String uri) {
        for (Rule r : rules) {
            if (r.method != null && !r.method.equalsIgnoreCase(method)) {
                continue;
            }
            if (r.pattern.matcher(uri).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 启动时编译一次，避免每个请求都拼正则
     */
    @javax.annotation.PostConstruct
    void compilePublicPaths() {
        rules.clear();
        for (String raw : publicPaths.split(",")) {
            String t = raw.trim();
            if (t.isEmpty()) {
                continue;
            }
            String method = null;
            int idx = t.indexOf(':');
            if (idx > 0) {
                method = t.substring(0, idx).trim().toUpperCase();
                t = t.substring(idx + 1).trim();
            }
            rules.add(new Rule(method, java.util.regex.Pattern.compile(toRegex(t))));
        }
    }

    private String extractToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + msg + "\"}");
    }

    /**
     * 一条白名单规则：method 为 null 表示不限方法
     */
    private static final class Rule {
        final String method;
        final java.util.regex.Pattern pattern;

        Rule(String method, java.util.regex.Pattern pattern) {
            this.method = method;
            this.pattern = pattern;
        }
    }
}
