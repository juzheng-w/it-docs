package com.tyeeli.it.docs.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swagger 2 文档配置（springfox 3.0）。
 *
 * <p>界面：/swagger-ui.html ，JSON：/v2/api-docs（可导入 Apifox / Postman）。</p>
 *
 * <p>Boot 2.6+ 兼容两件事缺一不可：</p>
 * <ol>
 *   <li>application.yml 配 <code>spring.mvc.pathmatch.matching-strategy=ant_path_matcher</code>；</li>
 *   <li>classpath 有 actuator 时，management 端点的 HandlerMapping 仍走 PathPatternParser，
 *       springfox 扫到会 NPE —— 下面的 {@link #springfoxHandlerProviderBeanPostProcessor()}
 *       把这类 mapping 从 springfox 的扫描列表里剔除（springfox issue #3467 官方社区方案）。</li>
 * </ol>
 */
@Configuration
@EnableSwagger2
public class Swagger2Config {

    /** 认证方案名，与 {@link #securityContext()} 里引用的一致 */
    private static final String SECURITY_SCHEME = "Authorization";

    /**
     * 剔除使用 PathPatternParser 的 HandlerMapping（actuator 端点），只留 Ant 路径的，
     * 避免 documentationPluginsBootstrapper 启动时 NPE
     */
    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider) {
                    List<RequestMappingInfoHandlerMapping> mappings = getHandlerMappings(bean);
                    List<RequestMappingInfoHandlerMapping> copy = mappings.stream()
                            .filter(mapping -> mapping.getPatternParser() == null)
                            .collect(Collectors.toList());
                    mappings.clear();
                    mappings.addAll(copy);
                }
                return bean;
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException("无法读取 WebMvcRequestHandlerProvider.handlerMappings", e);
                }
            }
        };
    }

    @Bean
    public Docket itDocsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.tyeeli.it.docs.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
                // 全局 Authorization 头：点右上角 Authorize 粘 token（不写 Bearer 前缀，拦截器按原样取）
                .securitySchemes(Collections.singletonList(
                        new ApiKey(SECURITY_SCHEME, "Authorization", "header")))
                .securityContexts(Collections.singletonList(securityContext()));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("it-docs 接口文档")
                .description("IT 文档知识共享平台后端（TMS/WMS 业务知识库）。")
                .version("v1.0.0")
                .contact(new Contact("IT 基础架构", "", ""))
                .build();
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(Collections.singletonList(
                        new SecurityReference(SECURITY_SCHEME,
                                new AuthorizationScope[]{new AuthorizationScope("global", "全部接口")})))
                .build();
    }
}
