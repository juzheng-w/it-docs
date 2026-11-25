package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.Meta;

import java.util.List;

/**
 * 字典数据访问（it_meta：业务系统/问题分类）。
 */
public interface MetaRepository {

    /**
     * 按类型取可用字典项，按 sort_no, id 排序；type 为空则返回全部。
     */
    List<Meta> list(String metaType);

    Meta findById(Long id);

    /**
     * 同类型下是否已有同名项（编辑时排除自身）。
     */
    boolean existsName(String metaType, String name, Long excludeId);

    void insert(Meta meta);

    void update(Meta meta);

    void deleteById(Long id);
}
