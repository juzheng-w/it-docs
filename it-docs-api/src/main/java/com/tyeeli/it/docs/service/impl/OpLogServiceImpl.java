package com.tyeeli.it.docs.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.dto.OpLogQuery;
import com.tyeeli.it.docs.repository.OpLogRepository;
import com.tyeeli.it.docs.repository.entity.OpLog;
import com.tyeeli.it.docs.service.OpLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审计日志：全部写操作留痕，含改前改后 JSON。
 * 日志写失败不阻断业务（审计挂了不该让用户存不了文档），只打 ERROR。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpLogServiceImpl implements OpLogService {

    private final OpLogRepository opLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 日志分页查询：本层只做业务编排，查询条件由 Repository 实现拼装
     */
    @Override
    public PageResult<OpLog> page(OpLogQuery query) {
        int page = query.pageNo();
        int size = query.pageSize();
        Page<OpLog> p = opLogRepository.page(query.getDocId(), query.getOperator(), page, size);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    @Override
    public void record(String opType, Long docId, String operator, Object before, Object after) {
        try {
            OpLog l = new OpLog();
            l.setOpType(opType);
            l.setDocId(docId);
            l.setOperator(operator);
            l.setBeforeJson(before == null ? null : objectMapper.writeValueAsString(before));
            l.setAfterJson(after == null ? null : objectMapper.writeValueAsString(after));
            opLogRepository.insert(l);
        } catch (Exception e) {
            log.error("审计日志写入失败 opType={} docId={} operator={}", opType, docId, operator, e);
        }
    }
}
