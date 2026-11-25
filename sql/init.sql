-- =====================================================================
-- IT 部知识共享服务平台（it-docs）· 数据库初始化
-- MySQL 8.0 / utf8mb4 / 12 张表（架构文档 v1.4 第五节）
-- 注意：FULLTEXT 必须带 WITH PARSER ngram，否则中文搜不到
-- 执行时机：仅在 MySQL 数据目录为空时由 docker-entrypoint-initdb.d 自动执行；
--           已初始化后修改本文件不会生效，需手工执行或重建 data/mysql。
-- =====================================================================
CREATE DATABASE IF NOT EXISTS it_docs DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE it_docs;

-- ---------------------------------------------------------------------
-- 1. 空间：PUBLIC 公共空间（技术文档/相关问题和业务文档）· PERSONAL 个人区域
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_space (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  space_name  VARCHAR(100) NOT NULL COMMENT '空间名',
  space_code  VARCHAR(50)  NOT NULL COMMENT '编码，唯一',
  space_type  VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PERSONAL',
  visibility  VARCHAR(20)  NOT NULL DEFAULT 'INTERNAL' COMMENT '可见性：PUBLIC 匿名可看 / INTERNAL 登录可看 / RESTRICTED 仅授权名单',
  description VARCHAR(500)          DEFAULT NULL,
  owner       VARCHAR(50)           DEFAULT NULL COMMENT 'PERSONAL 空间归属用户名',
  source_type VARCHAR(20)           DEFAULT 'MANUAL' COMMENT 'MANUAL/DINGTALK（二期）',
  source_id   VARCHAR(100)          DEFAULT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DELETED',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_space_code (space_code),
  KEY idx_type (space_type, status)
) ENGINE=InnoDB COMMENT='知识空间';

-- ---------------------------------------------------------------------
-- 2. 文档：qa（问题记录）/ req（需求）/ tech（技术）/ md（旧版兼容）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_doc (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  doc_no       VARCHAR(20)  NOT NULL COMMENT '业务编号，如 QA-2026110001',
  space_id     BIGINT       NOT NULL,
  doc_type     VARCHAR(10)  NOT NULL DEFAULT 'tech' COMMENT 'qa/req/tech/md(legacy)',
  title        VARCHAR(200) NOT NULL,
  biz_system   VARCHAR(50)           DEFAULT NULL COMMENT '工单归属的业务系统（qa）。列名不用 system：MySQL 8.0 里 SYSTEM 是保留字',
  category     VARCHAR(50)           DEFAULT NULL COMMENT '问题分类/业务场景（qa）',
  owner        VARCHAR(50)           DEFAULT NULL COMMENT '处理角色（qa，不是系统归属）',
  summary      VARCHAR(500)          DEFAULT NULL COMMENT '一句话答案',
  content_md   MEDIUMTEXT            DEFAULT NULL COMMENT 'Markdown 正文 / 处理步骤',
  content_json JSON                  DEFAULT NULL COMMENT 'qa 结构化字段：errorText/cause/sql/steps/tips/images',
  tags         VARCHAR(200)          DEFAULT NULL COMMENT '逗号分隔',
  status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/DELETED',
  source_type  VARCHAR(20)           DEFAULT 'MANUAL',
  source_id    VARCHAR(100)          DEFAULT NULL,
  created_by   VARCHAR(50)           DEFAULT NULL,
  updated_by   VARCHAR(50)           DEFAULT NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc_no (doc_no),
  KEY idx_space (space_id, status, updated_at),
  KEY idx_status_time (status, updated_at),
  FULLTEXT KEY ft_search (title, summary, content_md, tags) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='文档/问答卡';

CREATE TABLE IF NOT EXISTS it_doc_no_seq (
  seq_key    VARCHAR(32) NOT NULL COMMENT '类型前缀-年月，如 QA-202611',
  current_no INT         NOT NULL,
  PRIMARY KEY (seq_key)
) ENGINE=InnoDB COMMENT='文档编号月度序列';

-- ---------------------------------------------------------------------
-- 3. 版本：每次写操作存快照，支持回看/回滚
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_doc_version (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  doc_id       BIGINT      NOT NULL,
  version_no   INT         NOT NULL,
  snapshot_v   TINYINT     NOT NULL DEFAULT 2 COMMENT '快照结构版本：1=仅标题与正文（旧行），2=含摘要/系统/分类/处理角色/标签',
  title        VARCHAR(200),
  summary      VARCHAR(500)         DEFAULT NULL COMMENT '该版本的一句话答案',
  biz_system   VARCHAR(50)          DEFAULT NULL COMMENT '该版本的归属业务系统（列名与 it_doc 一致）',
  category     VARCHAR(50)          DEFAULT NULL COMMENT '该版本的问题分类/业务场景',
  owner        VARCHAR(50)          DEFAULT NULL COMMENT '该版本的处理角色（qa）',
  tags         VARCHAR(200)         DEFAULT NULL COMMENT '该版本的标签（逗号分隔）',
  content_md   MEDIUMTEXT,
  content_json JSON,
  operator     VARCHAR(50),
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_doc_ver (doc_id, version_no)
) ENGINE=InnoDB COMMENT='文档版本快照（完整内容字段）';

-- ---------------------------------------------------------------------
-- 4. 空间授权：subject_type = USER（直授）/ LDAP_GROUP（域组，仅 LDAP 用户生效）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_space_acl (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  space_id     BIGINT      NOT NULL,
  subject_type VARCHAR(20) NOT NULL COMMENT 'USER/LDAP_GROUP',
  subject_name VARCHAR(100) NOT NULL COMMENT '用户名 或 域组 CN',
  role         VARCHAR(20) NOT NULL DEFAULT 'VIEWER' COMMENT 'EDITOR/VIEWER',
  created_by   VARCHAR(50),
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_subject (space_id, subject_type, subject_name),
  KEY idx_space_subject (space_id, subject_type, subject_name)
) ENGINE=InnoDB COMMENT='空间权限';

-- 单篇文档授权：有授权项即收紧空间默认读取权限；空表时继承空间权限
CREATE TABLE IF NOT EXISTS it_doc_acl (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  doc_id       BIGINT      NOT NULL,
  subject_type VARCHAR(20) NOT NULL COMMENT 'USER/LDAP_GROUP',
  subject_name VARCHAR(100) NOT NULL COMMENT '用户名或域组 CN/DN',
  created_by   VARCHAR(50),
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc_subject (doc_id, subject_type, subject_name),
  KEY idx_subject_doc (subject_type, subject_name, doc_id)
) ENGINE=InnoDB COMMENT='单篇文档读取授权';

-- ---------------------------------------------------------------------
-- 5. 文件元数据：所有文件（图片/附件/头像）唯一一张表，本体在持久化目录（单机 local）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_file (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type     VARCHAR(20)  NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE/ATTACHMENT/AVATAR',
  origin_name  VARCHAR(255) NOT NULL,
  stored_name  VARCHAR(100) NOT NULL COMMENT 'sha256前16.ext',
  rel_path     VARCHAR(200) NOT NULL COMMENT 'files/2026/11/ab/cd/xxx',
  ext          VARCHAR(10),
  mime         VARCHAR(100),
  size_bytes   BIGINT       NOT NULL DEFAULT 0,
  sha256       CHAR(64)     NOT NULL,
  doc_id       BIGINT                DEFAULT NULL COMMENT '先传后绑，可空',
  uploaded_by  VARCHAR(50),
  status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DELETED',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sha_doc_uploader (sha256, doc_id, uploaded_by),
  KEY idx_doc (doc_id)
) ENGINE=InnoDB COMMENT='文件元数据（本体在数据目录/对象存储）';

-- ---------------------------------------------------------------------
-- 6. 用户：LDAP 域账号（不存密码，首登自动建档）+ 本地账号（BCrypt）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_user (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  username       VARCHAR(50) NOT NULL,
  display_name   VARCHAR(100),
  auth_source    VARCHAR(10) NOT NULL DEFAULT 'LDAP' COMMENT 'LDAP/LOCAL',
  password_hash  VARCHAR(100)         DEFAULT NULL COMMENT '仅 LOCAL：BCrypt',
  role           VARCHAR(20) NOT NULL DEFAULT 'VIEWER' COMMENT 'ADMIN/EDITOR/VIEWER',
  sys_scope      VARCHAR(500)         DEFAULT NULL COMMENT '数据权限：可见业务系统（逗号分隔），空=全部',
  email          VARCHAR(100),
  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  must_change_pwd TINYINT    NOT NULL DEFAULT 0,
  token_version  INT         NOT NULL DEFAULT 0 COMMENT '令牌版本：改密/重置后自增，旧 JWT 失效',
  last_login_at  DATETIME,
  created_by     VARCHAR(50),
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='用户（域账号 + 本地账号）';

-- ---------------------------------------------------------------------
-- 7. 操作日志：全部写操作留痕，含改前改后
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_op_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  op_type     VARCHAR(20)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/RESTORE/PUBLISH/LOGIN',
  doc_id      BIGINT                DEFAULT NULL,
  operator    VARCHAR(50)  NOT NULL,
  before_json JSON                  DEFAULT NULL,
  after_json  JSON                  DEFAULT NULL,
  op_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_time (op_time),
  KEY idx_doc (doc_id, op_time),
  KEY idx_op (operator, op_time)
) ENGINE=InnoDB COMMENT='审计日志';

-- ---------------------------------------------------------------------
-- 8. 钉钉群↔空间绑定（二期，一期只建表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_group_bind (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  dingtalk_chat_id  VARCHAR(100) NOT NULL,
  space_id          BIGINT       NOT NULL,
  remark            VARCHAR(200),
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chat (dingtalk_chat_id)
) ENGINE=InnoDB COMMENT='钉钉群绑定（二期）';

-- ---------------------------------------------------------------------
-- 9. 系统配置（键值对）：LDAP 等运行时可在管理界面修改的配置
--     界面保存后即生效（无需重启容器）；.env 环境变量仅作未配置时的兜底默认值
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_config (
  cfg_key    VARCHAR(50)  NOT NULL,
  cfg_value  VARCHAR(500)          DEFAULT NULL,
  remark     VARCHAR(200),
  updated_by VARCHAR(50),
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (cfg_key)
) ENGINE=InnoDB COMMENT='系统配置（LDAP 连接等）';

-- ---------------------------------------------------------------------
-- 10. 基础数据字典：业务系统 / 问题分类 / 标签，带颜色，编辑表单下拉用
--     meta_type = BIZ_SYSTEM（工单归属系统）/ CATEGORY（问题分类）/ TAG（标签）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS it_meta (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  meta_type  VARCHAR(20) NOT NULL COMMENT 'BIZ_SYSTEM/CATEGORY（标签不入字典，直接存文档 tags 字段）',
  parent_id  BIGINT               DEFAULT NULL COMMENT '父级：仅 CATEGORY，绑定 BIZ_SYSTEM 字典 id；空=通用（所有系统可选）',
  name       VARCHAR(50) NOT NULL COMMENT '字典项名称（文档里存的就是这个字符串）',
  color      VARCHAR(10)          DEFAULT NULL COMMENT '展示色，#RRGGBB，空则前端用默认色',
  sort_no    INT         NOT NULL DEFAULT 0 COMMENT '排序号，小的在前',
  status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(50),
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_name (meta_type, name),
  KEY idx_type (meta_type, status, sort_no)
) ENGINE=InnoDB COMMENT='基础数据字典（业务系统/问题分类）';

-- =====================================================================
-- 初始数据：三个公共空间（个人空间用户首次进个人区域时自动创建）
-- 查阅端只公开问题记录；业务文档 / 技术文档为 INTERNAL（登录可见）。
-- 初始 ADMIN 本地账号由应用启动时 DataInitializer 自动创建（admin/admin@123，
-- 首次登录强制改密），BCrypt 哈希在 Java 侧生成，不写死在 SQL 里。
-- =====================================================================
INSERT INTO it_space (space_name, space_code, space_type, visibility, description)
SELECT '问题记录', 'QA', 'PUBLIC', 'PUBLIC', '报错原文、原因与处理步骤（system=归属业务系统）'
WHERE NOT EXISTS (SELECT 1 FROM it_space WHERE space_code = 'QA');

INSERT INTO it_space (space_name, space_code, space_type, visibility, description)
SELECT '业务文档', 'REQ', 'PUBLIC', 'INTERNAL', '业务流程、操作手册、培训手册（Markdown）'
WHERE NOT EXISTS (SELECT 1 FROM it_space WHERE space_code = 'REQ');

INSERT INTO it_space (space_name, space_code, space_type, visibility, description)
SELECT '技术文档', 'TECH', 'PUBLIC', 'INTERNAL', '部署手册、操作手册、配置文档（Markdown）'
WHERE NOT EXISTS (SELECT 1 FROM it_space WHERE space_code = 'TECH');

-- 字典初始数据（已存在则跳过）
INSERT INTO it_meta (meta_type, name, color, sort_no)
SELECT 'BIZ_SYSTEM', t.name, t.color, t.sort_no FROM (
  SELECT 'SVS'      AS name, '#409EFF' AS color, 1 AS sort_no UNION ALL
  SELECT 'OTWB'                , '#67C23A', 2 UNION ALL
  SELECT 'MES'                 , '#E6A23C', 3 UNION ALL
  SELECT '钉钉'                 , '#0089FF', 4 UNION ALL
  SELECT '邮箱'                 , '#F56C6C', 5
) t WHERE NOT EXISTS (SELECT 1 FROM it_meta WHERE meta_type='BIZ_SYSTEM' AND name=t.name);

INSERT INTO it_meta (meta_type, name, color, sort_no)
SELECT 'CATEGORY', t.name, t.color, t.sort_no FROM (
  SELECT '账号权限'  AS name, '#409EFF' AS color, 1 AS sort_no UNION ALL
  SELECT '接口报错'              , '#F56C6C', 2 UNION ALL
  SELECT '报表异常'              , '#E6A23C', 3 UNION ALL
  SELECT '数据修正'              , '#67C23A', 4 UNION ALL
  SELECT '环境部署'              , '#909399', 5
) t WHERE NOT EXISTS (SELECT 1 FROM it_meta WHERE meta_type='CATEGORY' AND name=t.name);

-- 标签不建字典：标签就是文档上的 tags 字段（逗号分隔自由填写），颜色由前端按名称生成
