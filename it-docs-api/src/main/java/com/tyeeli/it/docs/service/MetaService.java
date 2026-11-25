package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.dto.MetaDTO;
import com.tyeeli.it.docs.dto.MetaSaveRequest;

import java.util.List;

/**
 * 基础数据字典（业务系统/问题分类，带颜色）。
 * 读：登录用户即可（编辑表单下拉用）；写：仅 ADMIN。
 * 标签不是字典：标签直接写在文档的 tags 字段里（逗号分隔，自由填写）。
 */
public interface MetaService {

    /**
     * 按类型列字典项（type 为空返回全部），按 sortNo 升序。
     */
    List<MetaDTO> list(String metaType);

    /**
     * 新增字典项。type 必须是 BIZ_SYSTEM / CATEGORY，同类型名称唯一。
     */
    MetaDTO create(MetaSaveRequest req, String operator);

    /**
     * 编辑：名称/颜色/排序；type 不可改。
     */
    MetaDTO update(Long id, MetaSaveRequest req, String operator);

    /**
     * 删除字典项。不影响历史文档（文档存的是名称字符串）。
     */
    void delete(Long id);
}
