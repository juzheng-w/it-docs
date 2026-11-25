package com.tyeeli.it.docs.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tyeeli.it.docs.repository.DocVersionRepository;
import com.tyeeli.it.docs.repository.entity.DocVersion;
import com.tyeeli.it.docs.repository.mapper.DocVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档版本快照数据访问。每次写操作追加一条，只增不改。
 *
 * <p>MyBatis-Plus 实现：直接委托 DocVersionMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class DocVersionRepositoryImpl implements DocVersionRepository {

    private final DocVersionMapper mapper;

    /**
     * 某文档的全部版本，按版本号倒序（最新在前）。
     */
    @Override
    public List<DocVersion> listByDoc(Long docId) {
        return mapper.selectList(new QueryWrapper<DocVersion>()
                .eq("doc_id", docId).orderByDesc("version_no"));
    }

    /**
     * 定位指定版本，回滚时读取快照内容。
     */
    @Override
    public DocVersion find(Long docId, int versionNo) {
        return mapper.selectOne(new QueryWrapper<DocVersion>()
                .eq("doc_id", docId).eq("version_no", versionNo));
    }

    /**
     * 取当前最大版本号，用于算下一个版本号；无历史时返回 null。
     */
    @Override
    public DocVersion latest(Long docId) {
        return mapper.selectOne(new QueryWrapper<DocVersion>()
                .eq("doc_id", docId).orderByDesc("version_no").last("LIMIT 1"));
    }

    /**
     * 追加一条版本快照。
     */
    @Override
    public void insert(DocVersion version) {
        mapper.insert(version);
    }
}
