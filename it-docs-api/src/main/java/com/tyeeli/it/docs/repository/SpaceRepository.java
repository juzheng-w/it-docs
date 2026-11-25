package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.Space;

import java.util.List;

/**
 * 空间数据访问。空间分 PUBLIC（公共协作）与 PERSONAL（个人私有）两类。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface SpaceRepository {
    /**
     * 匿名访客可见的公共空间（visibility=PUBLIC），按 id 升序。
     */
    List<Space> listPublic();

    /**
     * 全部启用的公共空间（含 INTERNAL / RESTRICTED 各可见性），按 id 升序。
     * 登录用户的可见范围由 Service 按可见性 + ACL 过滤。
     */
    List<Space> listActivePublic();

    /**
     * 管理视角全部空间（含停用，仅排除 DELETED），按 id 升序。
     */
    List<Space> listAll();

    /**
     * 按主键查询，不过滤状态。
     */
    Space findById(Long id);

    /**
     * 按空间编码查询，编码全局唯一（个人区域为 P-&lt;username&gt;）。
     */
    Space findByCode(String code);

    /**
     * 新增空间，回填自增主键。
     */
    void insert(Space space);

    /**
     * 按主键更新空间基本信息。
     */
    void update(Space space);
}
