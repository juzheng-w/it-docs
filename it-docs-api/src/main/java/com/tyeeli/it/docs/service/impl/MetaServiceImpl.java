package com.tyeeli.it.docs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.dto.MetaDTO;
import com.tyeeli.it.docs.dto.MetaSaveRequest;
import com.tyeeli.it.docs.repository.MetaRepository;
import com.tyeeli.it.docs.repository.entity.Meta;
import com.tyeeli.it.docs.service.MetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 字典服务：名称去空、颜色格式校验、同类型重名校验。
 * 删除为物理删除——文档表存的是名称字符串，字典删了不影响历史文档展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaServiceImpl implements MetaService {

    private final MetaRepository metaRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String COLOR_RE = "^#[0-9A-Fa-f]{6}$";

    @Override
    public List<MetaDTO> list(String metaType) {
        String type = StrUtil.trimToNull(metaType);
        if (type != null) {
            checkType(type);
        }
        List<MetaDTO> out = new ArrayList<>();
        for (Meta m : metaRepository.list(type)) {
            out.add(toDTO(m));
        }
        return out;
    }

    @Override
    public MetaDTO create(MetaSaveRequest req, String operator) {
        String type = checkType(StrUtil.trimToNull(req.getType()));
        String name = trimName(req.getName());
        String color = checkColor(req.getColor());
        Long parentId = checkParent(req.getParentId(), type);
        if (metaRepository.existsName(type, name, null)) {
            throw BizException.badRequest("同类型下已存在同名项：" + name);
        }
        Meta m = new Meta();
        m.setMetaType(type);
        m.setParentId(parentId);
        m.setName(name);
        m.setColor(color);
        m.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
        m.setStatus(Constants.STATUS_ACTIVE);
        m.setCreatedBy(operator);
        metaRepository.insert(m);
        log.info("新增字典项 type={}, name={}, parentId={}, operator={}", type, name, parentId, operator);
        return toDTO(m);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MetaDTO update(Long id, MetaSaveRequest req, String operator) {
        Meta m = metaRepository.findById(id);
        if (m == null) {
            throw BizException.notFound("字典项不存在");
        }
        String oldName = m.getName();
        String name = trimName(req.getName());
        String color = checkColor(req.getColor());
        Long parentId = checkParent(req.getParentId(), m.getMetaType());
        if (metaRepository.existsName(m.getMetaType(), name, id)) {
            throw BizException.badRequest("同类型下已存在同名项：" + name);
        }
        // 只改归属/名称/颜色/排序，类型不可改
        m.setParentId(parentId);
        m.setName(name);
        m.setColor(color);
        if (req.getSortNo() != null) {
            m.setSortNo(req.getSortNo());
        }
        metaRepository.update(m);
        if (Constants.META_BIZ_SYSTEM.equals(m.getMetaType()) && !oldName.equals(name)) {
            syncBizSystemRename(oldName, name);
        }
        log.info("编辑字典项 id={}, name={}, parentId={}, operator={}", id, name, parentId, operator);
        return toDTO(m);
    }

    /**
     * 业务系统改名：文档与成员的可见范围存的都是系统名字符串，不跟着改就成孤儿数据
     * （老文档从系统卡片里消失、限定过该系统的账号变成什么都看不到）。
     */
    private void syncBizSystemRename(String oldName, String newName) {
        int docs = jdbcTemplate.update(
                "UPDATE it_doc SET biz_system = ? WHERE biz_system = ?", newName, oldName);
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                // 用 LOCATE 做整段匹配：系统名里可能带 _ 等 LIKE 通配符
                "SELECT id, sys_scope FROM it_user " +
                        "WHERE LOCATE(CONCAT(',', ?, ','), CONCAT(',', sys_scope, ',')) > 0", oldName);
        int touched = 0;
        for (Map<String, Object> u : users) {
            String scope = (String) u.get("sys_scope");
            List<String> parts = new ArrayList<>();
            for (String s : scope.split(",")) {
                String v = s.trim().isEmpty() ? "" : (s.trim().equals(oldName) ? newName : s.trim());
                if (!v.isEmpty() && !parts.contains(v)) {
                    parts.add(v);
                }
            }
            String joined = String.join(",", parts);
            if (!joined.equals(scope)) {
                jdbcTemplate.update("UPDATE it_user SET sys_scope = ? WHERE id = ?", joined, u.get("id"));
                touched++;
            }
        }
        log.info("业务系统改名 {} → {}：同步文档 {} 条、成员可见范围 {} 人", oldName, newName, docs, touched);
    }

    @Override
    public void delete(Long id) {
        Meta m = metaRepository.findById(id);
        if (m == null) {
            throw BizException.notFound("字典项不存在");
        }
        metaRepository.deleteById(id);
        log.info("删除字典项 id={}, type={}, name={}", id, m.getMetaType(), m.getName());
    }

    private MetaDTO toDTO(Meta m) {
        MetaDTO dto = new MetaDTO();
        dto.setId(m.getId());
        dto.setType(m.getMetaType());
        dto.setParentId(m.getParentId());
        dto.setName(m.getName());
        dto.setColor(m.getColor());
        dto.setSortNo(m.getSortNo());
        return dto;
    }

    /** 归属校验：只有 CATEGORY 能绑，且父必须是 BIZ_SYSTEM 字典项；空=通用 */
    private Long checkParent(Long parentId, String type) {
        if (parentId == null) {
            return null;
        }
        if (!Constants.META_CATEGORY.equals(type)) {
            throw BizException.badRequest("只有问题分类可以设置归属系统");
        }
        Meta parent = metaRepository.findById(parentId);
        if (parent == null || !Constants.META_BIZ_SYSTEM.equals(parent.getMetaType())) {
            throw BizException.badRequest("归属系统不存在（必须是业务系统字典项）");
        }
        return parentId;
    }

    /** 类型白名单校验，返回校验后的值（标签不是字典类型，直接存文档 tags 字段） */
    private String checkType(String type) {
        if (Constants.META_BIZ_SYSTEM.equals(type) || Constants.META_CATEGORY.equals(type)) {
            return type;
        }
        throw BizException.badRequest("type 必须是 BIZ_SYSTEM / CATEGORY");
    }

    private String trimName(String name) {
        String n = StrUtil.trimToEmpty(name);
        if (n.isEmpty()) {
            throw BizException.badRequest("名称不能为空");
        }
        return n;
    }

    /** 颜色可空；填了必须是 #RRGGBB */
    private String checkColor(String color) {
        String c = StrUtil.trimToNull(color);
        if (c == null || c.isEmpty()) {
            return null;
        }
        if (!c.matches(COLOR_RE)) {
            throw BizException.badRequest("颜色格式错误，应为 #RRGGBB，如 #409EFF");
        }
        return c;
    }
}
