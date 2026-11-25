package com.tyeeli.it.docs.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.repository.SpaceRepository;
import com.tyeeli.it.docs.repository.entity.Space;
import com.tyeeli.it.docs.repository.mapper.SpaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 空间数据访问。空间分 PUBLIC（公共协作）与 PERSONAL（个人私有）两类。
 *
 * <p>MyBatis-Plus 实现：直接委托 SpaceMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class SpaceRepositoryImpl implements SpaceRepository {

    private final SpaceMapper mapper;

    /**
     * 匿名访客可见的公共空间：visibility=PUBLIC。
     */
    @Override
    public List<Space> listPublic() {
        return mapper.selectList(new QueryWrapper<Space>()
                .eq("space_type", Constants.SPACE_PUBLIC)
                .eq("status", Constants.STATUS_ACTIVE)
                .eq("visibility", Constants.SPACE_VIS_PUBLIC)
                .orderByAsc("id"));
    }

    /**
     * 全部启用的公共空间（含 INTERNAL / RESTRICTED），可见性判定收在 Service。
     */
    @Override
    public List<Space> listActivePublic() {
        return mapper.selectList(new QueryWrapper<Space>()
                .eq("space_type", Constants.SPACE_PUBLIC)
                .eq("status", Constants.STATUS_ACTIVE)
                .orderByAsc("id"));
    }

    /**
     * 管理视角全部空间（含停用，排除 DELETED）。
     */
    @Override
    public List<Space> listAll() {
        return mapper.selectList(new QueryWrapper<Space>()
                .ne("status", Constants.STATUS_DELETED)
                .orderByAsc("id"));
    }

    /**
     * 按主键查询，不过滤状态。
     */
    @Override
    public Space findById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 按空间编码查询，编码全局唯一（个人区域为 P-&lt;username&gt;）。
     */
    @Override
    public Space findByCode(String code) {
        return mapper.selectOne(new QueryWrapper<Space>().eq("space_code", code));
    }

    /**
     * 新增空间，回填自增主键。
     */
    @Override
    public void insert(Space space) {
        mapper.insert(space);
    }

    /**
     * 按主键更新空间基本信息。
     */
    @Override
    public void update(Space space) {
        mapper.updateById(space);
    }
}
