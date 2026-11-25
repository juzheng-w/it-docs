package com.tyeeli.it.docs.common;

/**
 * 全局常量：角色 / 状态 / 来源等枚举值集中一处，避免字符串散落
 */
public final class Constants {

    /**
     * 全局角色
     */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_EDITOR = "EDITOR";
    public static final String ROLE_VIEWER = "VIEWER";
    /**
     * 用户/空间/文档/文件状态
     */
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";
    public static final String STATUS_DELETED = "DELETED";
    public static final String STATUS_DRAFT = "DRAFT";
    /**
     * 账号来源
     */
    public static final String AUTH_LDAP = "LDAP";
    public static final String AUTH_LOCAL = "LOCAL";
    /**
     * 空间类型
     */
    public static final String SPACE_PUBLIC = "PUBLIC";
    public static final String SPACE_PERSONAL = "PERSONAL";

    /**
     * 空间可见性（仅公共空间用）：匿名可看 / 登录可看 / 仅授权名单
     */
    public static final String SPACE_VIS_PUBLIC = "PUBLIC";
    public static final String SPACE_VIS_INTERNAL = "INTERNAL";
    public static final String SPACE_VIS_RESTRICTED = "RESTRICTED";
    /**
     * 空间角色
     */
    public static final String ACL_EDITOR = "EDITOR";
    public static final String ACL_VIEWER = "VIEWER";
    /**
     * ACL 主体类型
     */
    public static final String SUBJECT_USER = "USER";
    public static final String SUBJECT_LDAP_GROUP = "LDAP_GROUP";
    /**
     * 文档类型
     */
    public static final String DOC_QA = "qa";
    public static final String DOC_MD = "md";
    public static final String DOC_REQUIREMENT = "req";
    public static final String DOC_TECHNICAL = "tech";
    /**
     * 版本快照结构版本：1 = 仅标题与正文（旧快照，回滚时不覆盖摘要/系统/分类/处理角色/标签）；
     * 2 = 标题、摘要、系统、分类、处理角色、标签、正文全量快照
     */
    public static final int SNAPSHOT_V_LEGACY = 1;
    public static final int SNAPSHOT_V_FULL = 2;
    /**
     * 文件业务类型
     */
    public static final String FILE_IMAGE = "IMAGE";
    public static final String FILE_ATTACHMENT = "ATTACHMENT";
    public static final String FILE_AVATAR = "AVATAR";
    /**
     * 上传白名单
     */
    public static final String[] IMAGE_EXTS = {"png", "jpg", "jpeg", "webp", "gif"};
    public static final String[] ATTACH_EXTS = {"docx", "doc", "xlsx", "xls", "pdf", "zip", "txt", "md", "pptx"};
    /**
     * 单文件上限（字节）：20 MB，可由配置覆盖
     */
    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    /**
     * 个人空间编码前缀：P-<username>
     */
    public static final String PERSONAL_SPACE_PREFIX = "P-";
    /**
     * 字典类型：业务系统（工单归属系统）/ 问题分类
     * 标签不入字典——标签直接写在文档的 tags 字段里（逗号分隔自由填写）
     */
    public static final String META_BIZ_SYSTEM = "BIZ_SYSTEM";
    public static final String META_CATEGORY = "CATEGORY";

    private Constants() {
    }
}
