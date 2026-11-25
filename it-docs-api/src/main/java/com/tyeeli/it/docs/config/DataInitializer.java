package com.tyeeli.it.docs.config;

import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.repository.entity.User;
import com.tyeeli.it.docs.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动自动创建本地管理员账号 admin / admin@123（首登强制改密）。
 * BCrypt 哈希在 Java 侧生成，不写死在 SQL 里；只在 it_user 为空时执行一次。
 * 同时自建 it_config 表（老库升级兜底：init.sql 只在首次初始化时执行）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureConfigTable();
        ensureDocNoSequenceTable();
        ensureMetaTable();
        ensureSpaceVisibility();
        ensureContentSpaces();
        ensureUserSysScope();
        ensureUserAuthColumns();
        ensureDocVersionColumns();
        if (userService.findByUsername("admin") != null) {
            return;
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User admin = new User();
        admin.setUsername("admin");
        admin.setDisplayName("系统管理员");
        admin.setAuthSource(Constants.AUTH_LOCAL);
        admin.setPasswordHash(encoder.encode("admin@123"));
        admin.setRole(Constants.ROLE_ADMIN);
        admin.setStatus(Constants.STATUS_ACTIVE);
        admin.setMustChangePwd(1);
        admin.setCreatedBy("system");
        userService.insertIfAbsent(admin);
        log.warn("初始管理员已创建：admin / admin@123（首次登录强制改密，请尽快修改）");
    }

    /** 新建内容编号按类型和年月递增；老库由启动时幂等补表。 */
    private void ensureDocNoSequenceTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS it_doc_no_seq (" +
                            "  seq_key VARCHAR(32) NOT NULL COMMENT '类型前缀-年月'," +
                            "  current_no INT NOT NULL," +
                            "  PRIMARY KEY (seq_key)" +
                            ") ENGINE=InnoDB COMMENT='文档编号月度序列'"
            );
        } catch (Exception e) {
            log.error("it_doc_no_seq 表自建失败: {}", e.getMessage());
        }
    }

    /**
     * 老库升级兜底：已部署过的库不会重跑 init.sql，这里补建 it_meta（业务系统/问题分类字典）
     * 并在对应类型为空时灌入初始数据（与 init.sql 种子一致）。标签不入字典。
     */
    private void ensureMetaTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS it_meta (" +
                            "  id         BIGINT      NOT NULL AUTO_INCREMENT," +
                            "  meta_type  VARCHAR(20) NOT NULL," +
                            "  parent_id  BIGINT               DEFAULT NULL," +
                            "  name       VARCHAR(50) NOT NULL," +
                            "  color      VARCHAR(10)          DEFAULT NULL," +
                            "  sort_no    INT         NOT NULL DEFAULT 0," +
                            "  status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'," +
                            "  created_by VARCHAR(50)," +
                            "  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (id)," +
                            "  UNIQUE KEY uk_type_name (meta_type, name)," +
                            "  KEY idx_type (meta_type, status, sort_no)" +
                            ") ENGINE=InnoDB COMMENT='基础数据字典（业务系统/问题分类）'");
            ensureMetaParentColumn();
            seedMeta("BIZ_SYSTEM", new Object[][]{
                    {"SVS", "#409EFF", 1}, {"OTWB", "#67C23A", 2}, {"MES", "#E6A23C", 3},
                    {"钉钉", "#0089FF", 4}, {"邮箱", "#F56C6C", 5}});
            seedMeta("CATEGORY", new Object[][]{
                    {"账号权限", "#409EFF", 1}, {"接口报错", "#F56C6C", 2}, {"报表异常", "#E6A23C", 3},
                    {"数据修正", "#67C23A", 4}, {"环境部署", "#909399", 5}});
            dropTagDict();
        } catch (Exception e) {
            log.error("it_meta 表自建失败: {}", e.getMessage());
        }
    }

    /** 老库升级：it_meta 已存在但没有 parent_id 列（分类绑定业务系统）时补列 */
    private void ensureMetaParentColumn() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_meta' AND COLUMN_NAME = 'parent_id'",
                    Integer.class);
            if (cnt != null && cnt == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE it_meta ADD COLUMN parent_id BIGINT DEFAULT NULL " +
                                "COMMENT '父级：仅 CATEGORY，绑定 BIZ_SYSTEM 字典 id；空=通用（所有系统可选）' AFTER meta_type");
                log.info("it_meta 已补 parent_id 列");
            }
        } catch (Exception e) {
            log.error("it_meta 补列失败: {}", e.getMessage());
        }
    }

    /** 某字典类型一条都没有时灌入初始数据（幂等） */
    private void seedMeta(String type, Object[][] rows) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM it_meta WHERE meta_type = ?", Integer.class, type);
        if (cnt == null || cnt > 0) {
            return;
        }
        for (Object[] r : rows) {
            jdbcTemplate.update(
                    "INSERT INTO it_meta (meta_type, name, color, sort_no, created_by) VALUES (?,?,?,?, 'system')",
                    type, r[0], r[1], r[2]);
        }
        log.info("it_meta 字典 [{}] 初始数据已灌入 {} 条", type, rows.length);
    }

    /**
     * 标签不再是字典类型（直接存在文档 tags 字段里）：
     * 清掉老库里遗留的 TAG 字典行，只删字典不动文档
     */
    private void dropTagDict() {
        try {
            int n = jdbcTemplate.update("DELETE FROM it_meta WHERE meta_type = 'TAG'");
            if (n > 0) {
                log.info("已清理历史 TAG 字典行 {} 条（标签改为直接存文档 tags 字段）", n);
            }
        } catch (Exception e) {
            log.error("清理 TAG 字典失败: {}", e.getMessage());
        }
    }

    /**
     * 老库升级兜底：已部署过的库不会重跑 init.sql，这里补建 it_config
     */
    private void ensureConfigTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS it_config (" +
                            "  cfg_key    VARCHAR(50)  NOT NULL," +
                            "  cfg_value  VARCHAR(500)          DEFAULT NULL," +
                            "  remark     VARCHAR(200)," +
                            "  updated_by VARCHAR(50)," +
                            "  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "  PRIMARY KEY (cfg_key)" +
                            ") ENGINE=InnoDB COMMENT='系统配置（LDAP 连接等）'");
        } catch (Exception e) {
            log.error("it_config 表自建失败: {}", e.getMessage());
        }
    }

    /**
     * 老库升级：it_user 补 sys_scope 列（数据权限：可见业务系统，逗号分隔，空=全部）。
     */
    private void ensureUserSysScope() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_user' AND COLUMN_NAME = 'sys_scope'",
                    Integer.class);
            if (cnt == null || cnt > 0) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE it_user ADD COLUMN sys_scope VARCHAR(500) DEFAULT NULL " +
                            "COMMENT '数据权限：可见业务系统（逗号分隔），空=全部' AFTER role");
            log.info("it_user 已补 sys_scope 列（空=全部系统可见，存量账号行为不变）");
        } catch (Exception e) {
            log.error("it_user 补 sys_scope 列失败: {}", e.getMessage());
        }
    }

    /**
     * 老库升级：it_user 补 token_version 列（改密/重置后自增，旧 JWT 失效）。
     * 存量账号默认 0，行为不变；脚本 sql/upgrade-2026-11-20-auth-hardening.sql 等价。
     */
    private void ensureUserAuthColumns() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_user' AND COLUMN_NAME = 'token_version'",
                    Integer.class);
            if (cnt != null && cnt > 0) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE it_user ADD COLUMN token_version INT NOT NULL DEFAULT 0 " +
                            "COMMENT '令牌版本：改密/重置后自增，旧 JWT 失效' AFTER must_change_pwd");
            log.info("it_user 已补 token_version 列");
        } catch (Exception e) {
            log.error("it_user 补 token_version 列失败: {}", e.getMessage());
        }
    }

    /**
     * 老库升级：it_doc_version 补全快照字段（摘要 / 归属系统 / 分类 / 处理角色 / 标签 + 结构版本）。
     * 旧行 snapshot_v 默认 1，回滚时只恢复标题与正文，不会清空其余字段；
     * 应用写入的新快照 snapshot_v=2，回滚按快照完整恢复。
     * 等价脚本 sql/upgrade-2026-11-23-version-snapshot.sql。
     */
    private void ensureDocVersionColumns() {
        addColumnIfMissing("it_doc_version", "snapshot_v",
                "ALTER TABLE it_doc_version ADD COLUMN snapshot_v TINYINT NOT NULL DEFAULT 1 " +
                        "COMMENT '快照结构版本：1=仅标题与正文（旧行），2=含摘要/系统/分类/处理角色/标签'");
        addColumnIfMissing("it_doc_version", "summary",
                "ALTER TABLE it_doc_version ADD COLUMN summary VARCHAR(500) DEFAULT NULL " +
                        "COMMENT '该版本的一句话答案'");
        addColumnIfMissing("it_doc_version", "biz_system",
                "ALTER TABLE it_doc_version ADD COLUMN biz_system VARCHAR(50) DEFAULT NULL " +
                        "COMMENT '该版本的归属业务系统（列名与 it_doc 一致）'");
        addColumnIfMissing("it_doc_version", "category",
                "ALTER TABLE it_doc_version ADD COLUMN category VARCHAR(50) DEFAULT NULL " +
                        "COMMENT '该版本的问题分类/业务场景'");
        addColumnIfMissing("it_doc_version", "owner",
                "ALTER TABLE it_doc_version ADD COLUMN owner VARCHAR(50) DEFAULT NULL " +
                        "COMMENT '该版本的处理角色（qa）'");
        addColumnIfMissing("it_doc_version", "tags",
                "ALTER TABLE it_doc_version ADD COLUMN tags VARCHAR(200) DEFAULT NULL " +
                        "COMMENT '该版本的标签（逗号分隔）'");
    }

    /**
     * 列不存在才执行 DDL；已存在则跳过，可重复调用
     */
    private void addColumnIfMissing(String table, String column, String ddl) {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (cnt != null && cnt > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("{}.{} 列已补齐", table, column);
        } catch (Exception e) {
            log.error("为 {}.{} 补列失败: {}", table, column, e.getMessage());
        }
    }

    /**
     * 内容分区（一次性迁移，it_config 记标记，只跑一次）：
     * 公共查阅区只放问题记录（QA 空间保持 PUBLIC）；
     * 业务文档、技术文档改为 INTERNAL，未登录不可见；
     * 新建 REQ 空间承接业务文档 / 培训手册。
     * 管理员之后在「空间与权限」里再调整，不会被这里覆盖。
     */
    private void ensureContentSpaces() {
        try {
            Integer done = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM it_config WHERE cfg_key = 'content_scope_v1'", Integer.class);
            if (done != null && done > 0) {
                return;
            }
            jdbcTemplate.update(
                    "INSERT INTO it_space (space_name, space_code, space_type, visibility, description) " +
                            "SELECT '业务文档', 'REQ', 'PUBLIC', 'INTERNAL', '业务流程、操作手册、培训手册（Markdown）' " +
                            "WHERE NOT EXISTS (SELECT 1 FROM it_space WHERE space_code = 'REQ')");
            jdbcTemplate.update(
                    "UPDATE it_space SET visibility = 'INTERNAL' " +
                            "WHERE space_type = 'PUBLIC' AND space_code IN ('TECH', 'REQ')");
            jdbcTemplate.update(
                    "UPDATE it_space SET space_name = '问题记录', " +
                            "description = '报错原文、原因与处理步骤（system=归属业务系统）' " +
                            "WHERE space_code = 'QA' AND space_name IN ('相关问题和业务文档', '相关问题')");
            jdbcTemplate.update(
                    "UPDATE it_space SET status = 'ACTIVE' " +
                            "WHERE space_type = 'PUBLIC' AND space_code IN ('TECH', 'REQ') AND status <> 'ACTIVE'");
            jdbcTemplate.update(
                    "INSERT INTO it_config (cfg_key, cfg_value, remark, updated_by) " +
                            "VALUES ('content_scope_v1', 'done', '内容可见性一次性迁移标记', 'system')");
            log.info("内容分区已调整：问题记录公开，业务/技术文档登录可见（content_scope_v1）");
        } catch (Exception e) {
            log.error("内容分区迁移失败: {}", e.getMessage());
        }
    }

    /**
     * 老库升级：it_space 补 visibility 列（PUBLIC 匿名 / INTERNAL 登录 / RESTRICTED 名单）。
     * 列是新加的这一轮里，把存量公共空间设为 PUBLIC——保持升级前匿名可看的原行为，
     * 管理员之后可在「空间管理」页自行收紧；个人空间不参与可见性。
     */
    private void ensureSpaceVisibility() {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_space' AND COLUMN_NAME = 'visibility'",
                    Integer.class);
            if (cnt == null || cnt > 0) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE it_space ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'INTERNAL' " +
                            "COMMENT '可见性：PUBLIC 匿名可看 / INTERNAL 登录可看 / RESTRICTED 仅授权名单' " +
                            "AFTER space_type");
            int n = jdbcTemplate.update(
                    "UPDATE it_space SET visibility = 'PUBLIC' WHERE space_type = 'PUBLIC'");
            log.info("it_space 已补 visibility 列，存量公共空间 {} 个保持 PUBLIC（原匿名可见行为）", n);
        } catch (Exception e) {
            log.error("it_space 补 visibility 列失败: {}", e.getMessage());
        }
    }
}
