-- 2026-11-23 版本快照补全：回滚时一并恢复摘要 / 归属系统 / 分类 / 处理角色 / 标签
-- 适用于已按旧版 init.sql 建库的环境；全新安装直接用 init.sql（已含这些列）。
-- 每个 ALTER 都先查 information_schema，可重复执行。
--
-- snapshot_v 说明：
--   旧行补列后为 1（仅标题与正文），回滚时不会覆盖其余字段；
--   应用写入的新快照为 2（全量内容字段），回滚按快照完整恢复。
-- 应用启动时 DataInitializer#ensureDocVersionColumns 会做同样的幂等补列，本脚本供手工执行 / 留档。

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_doc_version' AND COLUMN_NAME = 'snapshot_v');
SET @sql := IF(@c = 0,
  'ALTER TABLE it_doc_version ADD COLUMN snapshot_v TINYINT NOT NULL DEFAULT 1 COMMENT ''快照结构版本：1=仅标题与正文（旧行），2=含摘要/系统/分类/处理角色/标签''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_doc_version' AND COLUMN_NAME = 'summary');
SET @sql := IF(@c = 0,
  'ALTER TABLE it_doc_version ADD COLUMN summary VARCHAR(500) DEFAULT NULL COMMENT ''该版本的一句话答案''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_doc_version' AND COLUMN_NAME = 'biz_system');
SET @sql := IF(@c = 0,
  'ALTER TABLE it_doc_version ADD COLUMN biz_system VARCHAR(50) DEFAULT NULL COMMENT ''该版本的归属业务系统（列名与 it_doc 一致）''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_doc_version' AND COLUMN_NAME = 'category');
SET @sql := IF(@c = 0,
  'ALTER TABLE it_doc_version ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT ''该版本的问题分类/业务场景''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_doc_version' AND COLUMN_NAME = 'owner');
SET @sql := IF(@c = 0,
  'ALTER TABLE it_doc_version ADD COLUMN owner VARCHAR(50) DEFAULT NULL COMMENT ''该版本的处理角色（qa）''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_doc_version' AND COLUMN_NAME = 'tags');
SET @sql := IF(@c = 0,
  'ALTER TABLE it_doc_version ADD COLUMN tags VARCHAR(200) DEFAULT NULL COMMENT ''该版本的标签（逗号分隔）''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
