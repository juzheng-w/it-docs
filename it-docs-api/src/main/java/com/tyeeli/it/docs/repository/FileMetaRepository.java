package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.FileMeta;

/**
 * 附件元数据数据访问。文件本体在对象存储，库里只存相对路径与 sha256。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface FileMetaRepository {
    FileMeta findBySha256AndDocIdAndUploader(String sha256, Long docId, String uploader);

    /**
     * 按主键查询，不过滤状态。
     */
    FileMeta findById(Long id);

    /**
     * 新增文件元数据。
     */
    void insert(FileMeta file);

    /**
     * 按主键更新（复活已删记录、逻辑删除）。
     */
    void update(FileMeta file);
}
