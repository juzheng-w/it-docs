package com.tyeeli.it.docs.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.repository.OpLogRepository;
import com.tyeeli.it.docs.repository.entity.OpLog;
import com.tyeeli.it.docs.repository.mapper.OpLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 审计日志数据访问。只增不改不删，改前改后各存一份 JSON。
 *
 * <p>MyBatis-Plus 实现：直接委托 OpLogMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class OpLogRepositoryImpl implements OpLogRepository {

    private final OpLogMapper mapper;

    /**
     * 分页查询操作日志。docId 与 operator 都可为 null，按操作时间倒序。
     */
    @Override
    public Page<OpLog> page(Long docId, String operator, int pageNo, int pageSize) {
        QueryWrapper<OpLog> qw = new QueryWrapper<OpLog>();
        if (docId != null) {
            qw.eq("doc_id", docId);
        }
        if (StrUtil.isNotBlank(operator)) {
            qw.eq("operator", operator.trim());
        }
        qw.orderByDesc("op_time");
        return mapper.selectPage(new Page<OpLog>(pageNo, pageSize), qw);
    }

    /**
     * 追加一条审计记录。
     */
    @Override
    public void insert(OpLog log) {
        mapper.insert(log);
    }
}
