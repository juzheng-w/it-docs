package com.tyeeli.it.docs.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.repository.DocRepository;
import com.tyeeli.it.docs.repository.entity.Doc;
import com.tyeeli.it.docs.repository.mapper.DocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档数据访问。软删除用 status 标记，物理行永不删除，历史版本另表存放。
 *
 * <p>MyBatis-Plus 实现：直接委托 DocMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class DocRepositoryImpl implements DocRepository {

    private final DocMapper mapper;

    /**
     * 中文全文检索（FULLTEXT ngram）。命中上限 500 条，排序交给 SQL；空间与类型过滤由 Service 在内存里补，避免全文索引失效。
     */
    @Override
    public Page<Doc> searchByKeyword(String keyword, Long spaceId, List<Long> visibleSpaceIds,
                                     String system, String category, String docType,
                                     boolean includeDisabled, List<String> allowedSystems, List<Long> readableDocIds,
                                     List<Long> editableSpaceIds,
                                     int pageNo, int pageSize) {
        QueryWrapper<Doc> qw = new QueryWrapper<Doc>();
        qw.ne("status", Constants.STATUS_DELETED);
        if (!includeDisabled) qw.ne("status", Constants.STATUS_DISABLED);
        if (spaceId != null) {
            qw.eq("space_id", spaceId);
        } else if (visibleSpaceIds == null || visibleSpaceIds.isEmpty()) {
            return new Page<Doc>(pageNo, pageSize);
        } else {
            qw.in("space_id", visibleSpaceIds);
        }
        if (StrUtil.isNotBlank(system)) qw.eq("biz_system", system.trim());
        if (allowedSystems != null) {
            if (allowedSystems.isEmpty()) {
                qw.and(w -> w.isNull("biz_system").or().eq("biz_system", ""));
            } else {
                qw.and(w -> w.in("biz_system", allowedSystems)
                        .or().isNull("biz_system").or().eq("biz_system", ""));
            }
        }
        if (StrUtil.isNotBlank(category)) qw.eq("category", category.trim());
        if (StrUtil.isNotBlank(docType)) qw.eq("doc_type", docType.trim());
        applyDocAcl(qw, readableDocIds, editableSpaceIds);
        String kw = keyword.trim();
        qw.and(w -> w.apply("MATCH(title, summary, content_md, tags) AGAINST({0} IN NATURAL LANGUAGE MODE)", kw)
                .or().apply("CAST(content_json AS CHAR) LIKE CONCAT('%', {0}, '%')", kw));
        // 有明确查询词时优先按全文相关度排序；相关度相同再按更新时间。
        // content_json 的 LIKE 仅作为结构化字段兜底，索引列必须与 ft_search 保持一致。
        qw.last("ORDER BY MATCH(title, summary, content_md, tags) AGAINST (" + sqlLiteral(kw)
                + " IN NATURAL LANGUAGE MODE) DESC, updated_at DESC");
        // MATCH 相关度排序不适合被 count 优化器改写，关闭后由子查询计数，避免 ONLY_FULL_GROUP_BY 报错
        Page<Doc> page = new Page<Doc>(pageNo, pageSize);
        page.setOptimizeCountSql(false);
        return mapper.selectPage(page, qw);
    }

    /**
     * 分页查询文档，按更新时间倒序；软删的不返回，下架的看 includeDisabled。
     * 没有可读空间时返回空页，绝不能因为条件为空就把全表捞出来。
     * allowedSystems 非空时按账号数据权限收敛：归属这些系统的问答卡 + 无系统归属的文档。
     */
    @Override
    public Page<Doc> page(Long spaceId, List<Long> visibleSpaceIds, String system, String category,
                          String docType, boolean includeDisabled, List<String> allowedSystems, List<Long> readableDocIds,
                          List<Long> editableSpaceIds,
                          int pageNo, int pageSize) {
        QueryWrapper<Doc> qw = new QueryWrapper<Doc>();
        qw.ne("status", Constants.STATUS_DELETED);
        if (!includeDisabled) {
            qw.ne("status", Constants.STATUS_DISABLED);
        }
        if (spaceId != null) {
            qw.eq("space_id", spaceId);
        } else {
            if (visibleSpaceIds == null || visibleSpaceIds.isEmpty()) {
                return new Page<Doc>(pageNo, pageSize);
            }
            qw.in("space_id", visibleSpaceIds);
        }
        if (StrUtil.isNotBlank(system)) {
            qw.eq("biz_system", system.trim());
        }
        if (allowedSystems != null) {
            // 数据权限：限定系统的问答卡 + 无系统归属的普通文档（手册类不限）
            qw.and(w -> w.in("biz_system", allowedSystems)
                    .or().isNull("biz_system").or().eq("biz_system", ""));
        }
        if (StrUtil.isNotBlank(category)) {
            qw.eq("category", category.trim());
        }
        if (StrUtil.isNotBlank(docType)) {
            qw.eq("doc_type", docType.trim());
        }
        applyDocAcl(qw, readableDocIds, editableSpaceIds);
        qw.orderByDesc("updated_at");
        return mapper.selectPage(new Page<Doc>(pageNo, pageSize), qw);
    }

    /**
     * 转成 MySQL 字符串字面量。仅用于 ORDER BY 中的 MATCH 相关度表达式：
     * MyBatis-Plus 的 last() 不支持占位参数，因此这里做最小必要转义。
     */
    private String sqlLiteral(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    /** 文档存在 ACL 行时，只让匹配到的授权用户/组进入结果集。 */
    private void applyDocAcl(QueryWrapper<Doc> qw, List<Long> readableDocIds, List<Long> editableSpaceIds) {
        if (readableDocIds == null) return;
        qw.and(w -> {
            w.notInSql("id", "SELECT doc_id FROM it_doc_acl");
            if (readableDocIds != null && !readableDocIds.isEmpty()) w.or().in("id", readableDocIds);
            if (editableSpaceIds != null && !editableSpaceIds.isEmpty()) w.or().in("space_id", editableSpaceIds);
        });
    }

    /**
     * 按主键查询，不过滤状态——已删除文档是否可见由 Service 判定。
     */
    @Override
    public Doc findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Doc findByDocNo(String docNo) {
        if (StrUtil.isBlank(docNo)) {
            return null;
        }
        QueryWrapper<Doc> qw = new QueryWrapper<Doc>();
        qw.eq("doc_no", docNo).last("LIMIT 1");
        return mapper.selectOne(qw);
    }

    @Override
    public Doc findActiveByTitleAndSpace(String title, Long spaceId) {
        if (StrUtil.isBlank(title) || spaceId == null) {
            return null;
        }
        QueryWrapper<Doc> qw = new QueryWrapper<Doc>();
        qw.eq("title", title).eq("space_id", spaceId).ne("status", Constants.STATUS_DELETED)
                .last("LIMIT 1");
        return mapper.selectOne(qw);
    }

    /**
     * 新增文档，回填自增主键（版本快照依赖该 id）。
     */
    @Override
    public void insert(Doc doc) {
        mapper.insert(doc);
    }

    /**
     * 按主键全字段更新。状态流转（删除/发布/回滚）也走这里。
     */
    @Override
    public void update(Doc doc) {
        mapper.updateById(doc);
    }

    /**
     * 统计某空间下未删除文档数，空间列表页脚用。
     */
    @Override
    public int countBySpace(Long spaceId) {
        return mapper.countBySpace(spaceId);
    }
}
