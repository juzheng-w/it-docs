package com.tyeeli.it.docs.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * <p>
 * 分页插件单独成类，不要放在 WebConfig 里：
 * WebConfig 构造依赖 JwtInterceptor → UserService → Mapper → MybatisPlusAutoConfiguration
 * → 需要 MybatisPlusInterceptor，如果该 Bean 由 WebConfig 提供就形成循环，应用会起不来。
 */
@Configuration
public class MybatisConfig {

    /**
     * 分页插件（MySQL 8）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
