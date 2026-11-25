package com.tyeeli.it.docs.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色要求（Web 层统一鉴权，替代 Controller 里各写一份 requireAdmin）。
 * 可标在类（整类生效）或方法（覆盖类）上；由 JwtInterceptor 读取 HandlerMethod 判定。
 * 未登录 → 401；已登录但角色不符 → 403。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许的全局角色，命中任一即可
     */
    String[] value() default {};
}
