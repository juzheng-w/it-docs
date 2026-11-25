-- 首次部署后校验：表、空间、字典、管理员、关键列
-- 用法（在项目根目录）：
--   docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot it_docs' < sql/verify-schema.sql
-- 说明：admin 由 app 启动时 DataInitializer 创建，必须在 app healthy 后查。

USE it_docs;

SELECT 'table_count' AS metric, COUNT(*) AS metric_value
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name LIKE 'it_%';

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name LIKE 'it_%'
ORDER BY table_name;

SELECT space_code, space_name, visibility, status
FROM it_space
ORDER BY id;

SELECT meta_type, COUNT(*) AS item_count
FROM it_meta
GROUP BY meta_type
ORDER BY meta_type;

SELECT cfg_key, cfg_value
FROM it_config
WHERE cfg_key = 'content_scope_v1';

SELECT username, auth_source, role, status, must_change_pwd
FROM it_user
WHERE username = 'admin';

SELECT table_name, GROUP_CONCAT(column_name ORDER BY column_name) AS columns
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('it_doc_version', 'it_user', 'it_space')
GROUP BY table_name
ORDER BY table_name;
