package com.tyeeli.it.docs.service.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：MinIO 实现为主，local 兜底（仅开发期）。
 * 存储方案切换不影响已存内容——文档正文只存 /api/files/{id}/view 这样的 url。
 */
public interface StorageClient {

    /**
     * 上传（relPath 形如 files/2026/11/ab/cd/xxx.png），成功返回可访问的对象键
     */
    String put(String relPath, InputStream in, long size, String mime) throws Exception;

    /**
     * 读取对象内容
     */
    InputStream get(String relPath) throws Exception;

    /**
     * 删除对象（孤儿清理用，失败不阻断）
     */
    void remove(String relPath) throws Exception;

    /**
     * 当前实现名：minio / local
     */
    String type();
}
