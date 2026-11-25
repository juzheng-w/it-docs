package com.tyeeli.it.docs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：只放拦截器注册与跨域。
 * MyBatis-Plus 分页插件在 MybatisConfig（放这里会造成 Bean 循环依赖）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截 /api/**；公开路径在 JwtInterceptor 内部白名单判断（同一路径 GET 公开 / 写操作由 Service 二次鉴权）
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/api/**");
    }

    /**
     * 开发期前后端分离跨域；生产走 nginx 同域反代，此配置无副作用
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
