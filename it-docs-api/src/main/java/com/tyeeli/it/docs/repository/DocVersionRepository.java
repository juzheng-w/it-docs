package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.DocVersion;

import java.util.List;

/**
 * 文档版本快照数据访问。每次写操作追加一条，只增不改。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface DocVersionRepository {
    /**
     * 某文档的全部版本，按版本号倒序（最新在前）。
     */
    List<DocVersion> listByDoc(Long docId);

    /**
     * 定位指定版本，回滚时读取快照内容。
     */
    DocVersion find(Long docId, int versionNo);

    /**
     * 取当前最大版本号，用于算下一个版本号；无历史时返回 null。
     */
    DocVersion latest(Long docId);

    /**
     * 追加一条版本快照。
     */
    void insert(DocVersion version);
}
