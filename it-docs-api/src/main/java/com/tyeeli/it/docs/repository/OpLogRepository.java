package com.tyeeli.it.docs.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.repository.entity.OpLog;

/**
 * 审计日志数据访问。只增不改不删，改前改后各存一份 JSON。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface OpLogRepository {
    /**
     * 分页查询操作日志。docId 与 operator 都可为 null，按操作时间倒序。
     */
    Page<OpLog> page(Long docId, String operator, int pageNo, int pageSize);

    /**
     * 追加一条审计记录。
     */
    void insert(OpLog log);
}
