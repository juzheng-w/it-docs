-- 单篇文档授权表：没有授权行时继承所属空间的读取权限。
CREATE TABLE IF NOT EXISTS it_doc_acl (
  id BIGINT NOT NULL AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  subject_type VARCHAR(20) NOT NULL COMMENT 'USER/LDAP_GROUP',
  subject_name VARCHAR(100) NOT NULL COMMENT '用户名或域组 CN/DN',
  created_by VARCHAR(50),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc_subject (doc_id, subject_type, subject_name),
  KEY idx_subject_doc (subject_type, subject_name, doc_id)
) ENGINE=InnoDB COMMENT='单篇文档读取授权；存在授权项即收紧空间默认权限';

-- 同一文件可由不同文档分别引用；元数据按文档及上传人隔离，避免附件权限串文档。
-- 老库的 uk_sha 与新库的 idx_sha_doc_uploader 二选一：先判断再改，脚本可重复执行。

SET @old_idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'it_file' AND index_name = 'uk_sha'
);
SET @sql := IF(@old_idx > 0, 'ALTER TABLE it_file DROP INDEX uk_sha', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @new_idx := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'it_file' AND index_name = 'idx_sha_doc_uploader'
);
SET @sql := IF(@new_idx = 0,
  'ALTER TABLE it_file ADD KEY idx_sha_doc_uploader (sha256, doc_id, uploaded_by)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
