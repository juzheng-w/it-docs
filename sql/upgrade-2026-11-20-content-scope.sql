-- 内容分区：查阅端只公开问题记录，业务文档 / 技术文档改为登录可见
-- 应用启动时 DataInitializer#ensureContentSpaces 会做同样的幂等迁移，
-- 本脚本供手工执行 / 留档，执行一次即可。
-- 前置：无。本脚本自行确保 it_space.visibility 与 it_config 存在（老库可能没有）。

USE it_docs;

-- 0. 老库可能没有 it_config（内容迁移标记要用）
CREATE TABLE IF NOT EXISTS it_config (
  cfg_key    VARCHAR(50)  NOT NULL,
  cfg_value  VARCHAR(500)          DEFAULT NULL,
  remark     VARCHAR(200),
  updated_by VARCHAR(50),
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (cfg_key)
) ENGINE=InnoDB COMMENT='系统配置（LDAP 连接等）';

-- 1. 老库可能没有 it_space.visibility：缺列则补，且把历史 PUBLIC 空间置回 PUBLIC
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'it_space'
    AND COLUMN_NAME = 'visibility'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE it_space ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT ''INTERNAL'' COMMENT ''可见性：PUBLIC 匿名可看 / INTERNAL 登录可看 / RESTRICTED 仅授权名单'' AFTER space_type',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @seed := IF(@col_exists = 0,
  'UPDATE it_space SET visibility = ''PUBLIC'' WHERE space_type = ''PUBLIC'' AND space_code = ''QA''',
  'SELECT 1');
PREPARE stmt FROM @seed;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 新建业务文档空间（已存在则跳过）
INSERT INTO it_space (space_name, space_code, space_type, visibility, description)
SELECT '业务文档', 'REQ', 'PUBLIC', 'INTERNAL', '业务流程、操作手册、培训手册（Markdown）'
WHERE NOT EXISTS (SELECT 1 FROM it_space WHERE space_code = 'REQ');

-- 3. 业务文档、技术文档不对匿名开放
UPDATE it_space SET visibility = 'INTERNAL'
WHERE space_type = 'PUBLIC' AND space_code IN ('TECH', 'REQ');

-- 4. 原「相关问题和业务文档」空间只留问题记录
UPDATE it_space
SET space_name = '问题记录',
    description = '报错原文、原因与处理步骤（system=归属业务系统）'
WHERE space_code = 'QA' AND space_name IN ('相关问题和业务文档', '相关问题');

-- 5. 新分区需要业务/技术文档空间可用：处于停用状态的恢复（已启用则不动）
UPDATE it_space SET status = 'ACTIVE'
WHERE space_type = 'PUBLIC' AND space_code IN ('TECH', 'REQ') AND status <> 'ACTIVE';

-- 6. 应用侧的「只跑一次」标记（手工执行本脚本后补上，避免启动时再迁移一遍）
INSERT INTO it_config (cfg_key, cfg_value, remark, updated_by)
VALUES ('content_scope_v1', 'done', '内容可见性一次性迁移标记', 'system')
ON DUPLICATE KEY UPDATE cfg_value = 'done';
