package com.tyeeli.it.docs.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tyeeli.it.docs.repository.GroupBindRepository;
import com.tyeeli.it.docs.repository.entity.GroupBind;
import com.tyeeli.it.docs.repository.mapper.GroupBindMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 群绑定数据访问（二期：钉钉群 ↔ 空间）。一期只建表，供机器人推送时反查空间。
 *
 * <p>MyBatis-Plus 实现：直接委托 GroupBindMapper，单表 CRUD 不写 XML，
 * 只有需要 FULLTEXT 的语句才落在 Mapper 的注解 SQL 上。</p>
 */
@Repository
@RequiredArgsConstructor
public class GroupBindRepositoryImpl implements GroupBindRepository {

    private final GroupBindMapper mapper;

    /**
     * 按钉钉群会话 id 反查绑定的空间。
     */
    @Override
    public GroupBind findByChatId(String chatId) {
        return mapper.selectOne(new QueryWrapper<GroupBind>().eq("dingtalk_chat_id", chatId));
    }

    /**
     * 新增一条群绑定。
     */
    @Override
    public void insert(GroupBind bind) {
        mapper.insert(bind);
    }
}
