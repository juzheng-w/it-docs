-- 2026-11-20 认证加固：登录时间落库、改密后旧 JWT 失效
-- 适用于已按旧版 init.sql 建库的环境；全新安装直接用 init.sql。
-- 每个 ALTER 都先查 information_schema，可重复执行。

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'it_user' AND COLUMN_NAME = 'token_version');
SET @sql := IF(@c = 0,
  'ALTER TABLE it_user ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT ''令牌版本：改密/重置后自增，旧 JWT 失效'' AFTER must_change_pwd',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
