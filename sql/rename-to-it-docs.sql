-- =====================================================================
-- 命名迁移：it_kb + kb_*  →  it_docs + it_*
-- 只在「本地/测试库已按旧命名建过」时执行一次；全新环境直接跑 init.sql。
-- 生产库未上线，不提供在线双写方案。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS it_docs
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

RENAME TABLE
  it_kb.kb_space       TO it_docs.it_space,
  it_kb.kb_doc         TO it_docs.it_doc,
  it_kb.kb_doc_version TO it_docs.it_doc_version,
  it_kb.kb_space_acl   TO it_docs.it_space_acl,
  it_kb.kb_file        TO it_docs.it_file,
  it_kb.kb_user        TO it_docs.it_user,
  it_kb.kb_op_log      TO it_docs.it_op_log,
  it_kb.kb_group_bind  TO it_docs.it_group_bind,
  it_kb.kb_config      TO it_docs.it_config;

-- 确认（应返回 9 行 it_*）
SELECT table_name FROM information_schema.tables
 WHERE table_schema = 'it_docs' ORDER BY table_name;

-- 确认无误后再删旧库（可选，建议先备份）
-- DROP DATABASE it_kb;
