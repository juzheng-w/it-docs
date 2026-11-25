package com.tyeeli.it.docs.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tyeeli.it.docs.repository.SpaceAclRepository;
import com.tyeeli.it.docs.repository.entity.SpaceAcl;
import com.tyeeli.it.docs.repository.mapper.SpaceAclMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 空间权限（ACL）数据访问。主体可以是具体用户（USER）或域组（LDAP_GROUP）。
 *
 * <p>MyBatis-Plus 实现：直接委托 SpaceAclMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class SpaceAclRepositoryImpl implements SpaceAclRepository {

    private final SpaceAclMapper mapper;

    /**
     * 某空间的全部授权记录，读权限判定每次只查这一批（上层有 5 分钟缓存）。
     */
    @Override
    public List<SpaceAcl> listBySpace(Long spaceId) {
        return mapper.selectList(new QueryWrapper<SpaceAcl>().eq("space_id", spaceId));
    }

    /**
     * 清空某空间授权，配合全量覆盖式保存。
     */
    @Override
    public void deleteBySpace(Long spaceId) {
        mapper.delete(new QueryWrapper<SpaceAcl>().eq("space_id", spaceId));
    }

    /**
     * 新增一条授权。
     */
    @Override
    public void insert(SpaceAcl acl) {
        mapper.insert(acl);
    }
}
