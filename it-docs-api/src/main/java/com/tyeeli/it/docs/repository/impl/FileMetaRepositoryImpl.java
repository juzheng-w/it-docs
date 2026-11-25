package com.tyeeli.it.docs.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tyeeli.it.docs.repository.FileMetaRepository;
import com.tyeeli.it.docs.repository.entity.FileMeta;
import com.tyeeli.it.docs.repository.mapper.FileMetaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 附件元数据数据访问。文件本体在对象存储，库里只存相对路径与 sha256。
 *
 * <p>MyBatis-Plus 实现：直接委托 FileMetaMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class FileMetaRepositoryImpl implements FileMetaRepository {

    private final FileMetaMapper mapper;

    @Override
    public FileMeta findBySha256AndDocIdAndUploader(String sha256, Long docId, String uploader) {
        QueryWrapper<FileMeta> qw = new QueryWrapper<FileMeta>().eq("sha256", sha256)
                .eq("uploaded_by", uploader);
        if (docId == null) qw.isNull("doc_id"); else qw.eq("doc_id", docId);
        return mapper.selectOne(qw.last("LIMIT 1"));
    }

    /**
     * 按主键查询，不过滤状态。
     */
    @Override
    public FileMeta findById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 新增文件元数据。
     */
    @Override
    public void insert(FileMeta file) {
        mapper.insert(file);
    }

    /**
     * 按主键更新（复活已删记录、逻辑删除）。
     */
    @Override
    public void update(FileMeta file) {
        mapper.updateById(file);
    }
}
