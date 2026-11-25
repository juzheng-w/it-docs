package com.tyeeli.it.docs.config;

/**
 * 当前登录用户上下文（ThreadLocal，JwtInterceptor 注入，Controller/Service 读取）
 */
public class AuthContext {

    public static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<LoginUser>();

    public static void set(LoginUser u) {
        HOLDER.set(u);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 必须已登录，否则 401
     */
    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new com.tyeeli.it.docs.common.BizException(401, "请先登录");
        }
        return u;
    }

    public static boolean isAdmin() {
        LoginUser u = HOLDER.get();
        return u != null && com.tyeeli.it.docs.common.Constants.ROLE_ADMIN.equals(u.role);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static class LoginUser {
        public final Long uid;
        public final String username;
        public final String displayName;
        public final String role;
        public final boolean ldapUser;

        public LoginUser(Long uid, String username, String displayName, String role, boolean ldapUser) {
            this.uid = uid;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
            this.ldapUser = ldapUser;
        }
    }
}
