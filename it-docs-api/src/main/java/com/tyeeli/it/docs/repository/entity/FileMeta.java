package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件元数据：正文图 / 附件 / 头像共用，实体内容存对象存储。
 * sha256 用于对象存储内容寻址；元数据按文档和上传人隔离，避免附件权限串文档。
 */
@Data
@TableName("it_file")
public class FileMeta {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 业务类型：IMAGE 正文图 / ATTACHMENT 附件 / AVATAR 头像
     */
    private String bizType;
    /**
     * 原始文件名，下载时还原给客户端
     */
    private String originName;
    /**
     * 存储文件名（uuid + 扩展名），避免同名互相覆盖
     */
    private String storedName;
    /**
     * 对象存储相对路径；local 存储时相对根目录
     */
    private String relPath;
    /**
     * 扩展名，小写且不含点
     */
    private String ext;
    /**
     * MIME 类型，预览与下载时定 Content-Type
     */
    private String mime;
    /**
     * 文件大小（字节）
     */
    private Long sizeBytes;
    /**
     * 内容哈希，用于秒传与去重
     */
    private String sha256;
    /**
     * 关联文档 ID；编辑器草稿图为空
     */
    private Long docId;
    /**
     * 上传人用户名
     */
    private String uploadedBy;
    /**
     * 状态：ACTIVE 正常 / DELETED 逻辑删除
     */
    private String status;
    /**
     * 上传时间
     */
    private Date createdAt;
}
