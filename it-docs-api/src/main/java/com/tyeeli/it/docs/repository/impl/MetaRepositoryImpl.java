package com.tyeeli.it.docs.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.repository.MetaRepository;
import com.tyeeli.it.docs.repository.entity.Meta;
import com.tyeeli.it.docs.repository.mapper.MetaMapper;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字典数据访问实现：MyBatis-Plus 单表 CRUD，不写 XML。
 */
@Repository
@RequiredArgsConstructor
public class MetaRepositoryImpl implements MetaRepository {

    private final MetaMapper mapper;

    @Override
    public List<Meta> list(String metaType) {
        QueryWrapper<Meta> qw = new QueryWrapper<>();
        qw.eq("status", Constants.STATUS_ACTIVE);
        if (StrUtil.isNotBlank(metaType)) {
            qw.eq("meta_type", metaType);
        }
        qw.orderByAsc("sort_no", "id");
        return mapper.selectList(qw);
    }

    @Override
    public Meta findById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public boolean existsName(String metaType, String name, Long excludeId) {
        QueryWrapper<Meta> qw = new QueryWrapper<>();
        qw.eq("meta_type", metaType).eq("name", name);
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        return mapper.selectCount(qw) > 0;
    }

    @Override
    public void insert(Meta meta) {
        mapper.insert(meta);
    }

    @Override
    public void update(Meta meta) {
        mapper.updateById(meta);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
