package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.SpaceAcl;

import java.util.List;

/**
 * 空间权限（ACL）数据访问。主体可以是具体用户（USER）或域组（LDAP_GROUP）。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface SpaceAclRepository {
    /**
     * 某空间的全部授权记录，读权限判定每次只查这一批（上层有 5 分钟缓存）。
     */
    List<SpaceAcl> listBySpace(Long spaceId);

    /**
     * 清空某空间授权，配合全量覆盖式保存。
     */
    void deleteBySpace(Long spaceId);

    /**
     * 新增一条授权。
     */
    void insert(SpaceAcl acl);
}
