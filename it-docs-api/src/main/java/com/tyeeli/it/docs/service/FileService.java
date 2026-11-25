package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.repository.entity.FileMeta;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 附件服务：白名单校验 → 文档/上传人范围内去重 → 存对象存储 → 写元数据。
 *
 * <p>正文只保存 /api/files/{id}/view 这类相对地址，不落物理路径，
 * 换存储方案（minio ↔ local）不影响已存内容。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.FileServiceImpl}</p>
 */
public interface FileService {

    /**
     * 上传文件。同哈希文件直接复用已有记录（秒传）。
     *
     * @param bizType IMAGE（图片白名单）或 ATTACHMENT（附件白名单）
     */
    FileMeta upload(MultipartFile file, String bizType, Long docId, String operator, String globalRole, boolean ldapUser);

    /**
     * 取文件元数据，不存在或已删抛 404。
     */
    FileMeta mustGet(Long id);

    /**
     * 取「可读」文件：文件若挂在某篇文档下，要求该文档对当前用户可读；
     * 未挂文档的（编辑器草稿图、头像）直接放行。
     *
     * <p>view / download 是免登录接口，必须在这里做二次鉴权，
     * 否则拿到自增 id 就能越权取个人区域的图片与附件。</p>
     */
    FileMeta mustReadable(Long id, String currentUser, String globalRole, boolean ldapUser);

    void bindToDoc(Long id, Long docId, String operator, String globalRole, boolean ldapUser);

    /**
     * 打开对象流，供 Controller 边读边写响应。
     */
    InputStream openStream(FileMeta file);

    /**
     * 逻辑删除，物理对象由孤儿清理任务处理。
     */
    void softDelete(Long id, String operator, String globalRole, boolean ldapUser);
}
