package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 二期：钉钉群与空间的绑定关系（一期只建表，不参与业务）。
 */
@Data
@TableName("it_group_bind")
public class GroupBind {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 钉钉群 chatId
     */
    private String dingtalkChatId;
    /**
     * 绑定的空间 ID，指向 it_space
     */
    private Long spaceId;
    /**
     * 备注
     */
    private String remark;
    /**
     * 绑定时间
     */
    private Date createdAt;
}
