package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.dto.OpLogQuery;
import com.tyeeli.it.docs.repository.entity.OpLog;

/**
 * 审计日志：全部写操作留痕，改前改后各存一份 JSON。
 *
 * <p>日志写失败不阻断业务（审计挂了不该让用户存不了文档），只打 ERROR。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.OpLogServiceImpl}</p>
 */
public interface OpLogService {

    /**
     * 日志分页查询（查询条件收在本层，Controller 不拼 SQL）。
     */
    PageResult<OpLog> page(OpLogQuery query);

    /**
     * 记录一条审计。before/after 可为 null，对象会被序列化成 JSON。
     */
    void record(String opType, Long docId, String operator, Object before, Object after);
}
