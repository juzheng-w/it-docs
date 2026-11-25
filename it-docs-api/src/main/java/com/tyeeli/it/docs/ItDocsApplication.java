package com.tyeeli.it.docs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IT 部知识共享服务平台后端入口。
 * Java 8 + Spring Boot 2.7.18，默认端口 8080。
 * <p>
 * 放在根包下：@SpringBootApplication 默认扫描本包及子包，
 * api / service / repository / common 全部覆盖，无需再写 scanBasePackages。
 */
@SpringBootApplication
public class ItDocsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItDocsApplication.class, args);
    }
}
