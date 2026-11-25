package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文档版本快照：每次内容变更前落一条，用于历史回滚。
 * 只存内容相关字段（标题/摘要/系统/分类/处理角色/标签/正文），空间与状态变化不产生版本。
 * snapshot_v 标记快照结构版本：1 = 仅标题与正文（旧行），2 = 上述全部内容字段。
 */
@Data
@TableName("it_doc_version")
public class DocVersion {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 所属文档 ID，指向 it_doc
     */
    private Long docId;
    /**
     * 版本号，同一文档内自增，从 1 开始
     */
    private Integer versionNo;
    /**
     * 快照结构版本：1 = 仅标题与正文（旧行，回滚时不覆盖其余字段），2 = 全部内容字段
     */
    private Integer snapshotV;
    /**
     * 该版本的标题快照
     */
    private String title;
    /**
     * 该版本的一句话答案快照
     */
    private String summary;
    /**
     * 该版本的归属业务系统快照（列名 biz_system，与 it_doc 一致）
     */
    @TableField("biz_system")
    private String system;
    /**
     * 该版本的问题分类 / 业务场景快照
     */
    private String category;
    /**
     * 该版本的处理角色快照（qa）
     */
    private String owner;
    /**
     * 该版本的标签快照（英文逗号分隔）
     */
    private String tags;
    /**
     * 该版本的 Markdown 正文快照
     */
    private String contentMd;
    /**
     * 该版本的结构化字段快照（qa）
     */
    private String contentJson;
    /**
     * 产生该版本的操作用户
     */
    private String operator;
    /**
     * 版本生成时间
     */
    private Date createdAt;
}
