package com.tyeeli.it.docs.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.repository.entity.Doc;

import java.util.List;

/**
 * 文档数据访问。软删除用 status 标记，物理行永不删除，历史版本另表存放。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface DocRepository {
    /**
     * 中文全文检索（FULLTEXT ngram）。命中上限 500 条，排序交给 SQL；空间与类型过滤由 Service 在内存里补，避免全文索引失效。
     */
    /** 权限、筛选和分页均在数据库执行的全文检索。 */
    Page<Doc> searchByKeyword(String keyword, Long spaceId, List<Long> visibleSpaceIds,
                              String system, String category, String docType,
                              boolean includeDisabled, List<String> allowedSystems, List<Long> readableDocIds,
                              List<Long> editableSpaceIds,
                              int pageNo, int pageSize);

    /**
     * 分页查询文档，按更新时间倒序；已软删（DELETED）永不返回。
     *
     * @param spaceId         指定空间时直接按该空间过滤
     * @param visibleSpaceIds spaceId 为空时用它收敛可见范围（公共空间 + 本人个人区域）；
     *                        集合为空表示无可读空间，直接返回空页，不会退化成「查全表」
     * @param includeDisabled true 连已下架（DISABLED）一起返回——工作台恢复用；查阅端传 false
     * @param allowedSystems  账号数据权限（可见业务系统）；null=不限，非空=只出这些系统的
     *                        问答卡 + 无系统归属的普通文档
     */
    Page<Doc> page(Long spaceId, List<Long> visibleSpaceIds, String system, String category,
                   String docType, boolean includeDisabled, List<String> allowedSystems, List<Long> readableDocIds,
                   List<Long> editableSpaceIds,
                   int pageNo, int pageSize);

    /**
     * 按主键查询，不过滤状态——已删除文档是否可见由 Service 判定。
     */
    Doc findById(Long id);

    /**
     * 按业务编号查询（导入去重/更新用）。
     */
    Doc findByDocNo(String docNo);

    /**
     * 同空间下按标题查未删除文档（导入去重兜底：编号换了也能识别重复）。
     */
    Doc findActiveByTitleAndSpace(String title, Long spaceId);

    /**
     * 新增文档，回填自增主键（版本快照依赖该 id）。
     */
    void insert(Doc doc);

    /**
     * 按主键全字段更新。状态流转（删除/发布/回滚）也走这里。
     */
    void update(Doc doc);

    /**
     * 统计某空间下未删除文档数，空间列表页脚用。
     */
    int countBySpace(Long spaceId);
}
