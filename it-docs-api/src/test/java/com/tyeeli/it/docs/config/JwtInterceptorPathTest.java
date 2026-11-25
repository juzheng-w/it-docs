package com.tyeeli.it.docs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公开路径匹配规则回归测试。
 *
 * <p>背景：旧实现用 startsWith 前缀匹配，白名单里的 GET:/api/files 会连带放行
 * /api/files/{id}/download，导致个人区附件可被匿名下载。改成通配匹配后，
 * 这里锁死「只放行 view」「detail/versions 可匿名、写操作不可」这几条边界。</p>
 */
class JwtInterceptorPathTest {

    private static boolean match(String pattern, String uri) {
        return Pattern.compile(JwtInterceptor.toRegex(pattern)).matcher(uri).matches();
    }

    @Test
    @DisplayName("附件：view 放行、download 不放行")
    void fileViewOnly() {
        assertTrue(match("/api/files/*/view", "/api/files/12/view"));
        assertFalse(match("/api/files/*/view", "/api/files/12/download"));
        assertFalse(match("/api/files/*/view", "/api/files/12/view/extra"));
    }

    @Test
    @DisplayName("文档：列表与详情可匿名，子路径需单独声明")
    void docPaths() {
        assertTrue(match("/api/docs", "/api/docs"));
        assertFalse(match("/api/docs", "/api/docs/12"));
        assertTrue(match("/api/docs/**", "/api/docs/12"));
        assertTrue(match("/api/docs/**", "/api/docs/12/versions"));
    }

    @Test
    @DisplayName("普通路径完全匹配，不做前缀放行")
    void plainPathIsExact() {
        assertTrue(match("/error", "/error"));
        assertFalse(match("/error", "/error/internal"));
        assertFalse(match("/api/files", "/api/files/12/download"));
    }

    @Test
    @DisplayName("单段通配不跨 /")
    void singleSegmentWildcard() {
        assertFalse(match("/api/files/*/view", "/api/files/a/b/view"));
        assertTrue(match("/api/files/*/view", "/api/files/999/view"));
    }
}
