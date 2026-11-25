package com.tyeeli.it.docs.repository;

import com.tyeeli.it.docs.repository.entity.GroupBind;

/**
 * 群绑定数据访问（二期：钉钉群 ↔ 空间）。一期只建表，供机器人推送时反查空间。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface GroupBindRepository {
    /**
     * 按钉钉群会话 id 反查绑定的空间。
     */
    GroupBind findByChatId(String chatId);

    /**
     * 新增一条群绑定。
     */
    void insert(GroupBind bind);
}
