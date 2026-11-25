-- 2026-11-18 升级：全文索引加入 summary（一句话答案 / 摘要）
-- 适用于已按旧版 init.sql 建库的环境；新库直接使用 init.sql 即可。
-- 执行前请确认 it_doc 上存在 ft_search 索引；脚本会先判断再删除，可重复执行。

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'it_doc'
    AND index_name = 'ft_search'
);

SET @drop_sql := IF(@idx_exists > 0, 'ALTER TABLE it_doc DROP INDEX ft_search', 'SELECT 1');
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE it_doc
  ADD FULLTEXT KEY ft_search (title, summary, content_md, tags) WITH PARSER ngram;
