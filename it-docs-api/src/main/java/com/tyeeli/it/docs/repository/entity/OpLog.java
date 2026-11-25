package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 操作审计表：所有写操作与登录留痕，before/after 存 JSON 快照便于追查。
 */
@Data
@TableName("it_op_log")
public class OpLog {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 操作类型：CREATE/UPDATE/DELETE/RESTORE/PUBLISH/LOGIN
     */
    private String opType;
    /**
     * 关联文档 ID；登录等无文档场景为空
     */
    private Long docId;
    /**
     * 操作人用户名
     */
    private String operator;
    /**
     * 变更前快照 JSON，新建时为空
     */
    private String beforeJson;
    /**
     * 变更后快照 JSON，删除后仍保留最后状态
     */
    private String afterJson;
    /**
     * 操作时间
     */
    private Date opTime;
}
