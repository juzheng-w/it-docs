package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文档主表：问题记录、业务/技术文档与旧版 md 共用一张表，差异字段由 docType 区分。
 * 正文（contentMd）与结构化字段（contentJson）分列存储，列表查询不取正文以控制响应体积。
 */
@Data
@TableName(value = "it_doc", autoResultMap = true)
public class Doc {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 文档编号，服务端按类型前缀 + 年月 + 月内序号生成（如 QA-2026110001），对外展示与引用用。
     * 历史编号保持不变，避免已分享链接失效。
     */
    private String docNo;
    /**
     * 所属空间 ID，指向 it_space
     */
    private Long spaceId;
    /**
     * 文档类型：qa 问题记录 / req 业务文档 / tech 技术文档 / md 旧版兼容
     */
    private String docType;
    /**
     * 标题
     */
    private String title;
    /**
     * 工单归属的业务系统（仅 qa，如 WMS/ERP/SRM），不是处理落点。
     * 列名 biz_system：MySQL 8.0 里 SYSTEM 是保留字，JSON 字段名仍是 system 不影响前端
     */
    @TableField("biz_system")
    private String system;
    /**
     * 问题分类 / 业务场景（仅 qa）
     */
    private String category;
    /**
     * 处理角色（仅 qa），与 system 区分：system 是问题归属，owner 是处理人角色
     */
    private String owner;
    /**
     * 一句话答案，列表与搜索结果中展示
     */
    private String summary;
    /**
     * Markdown 正文（md 文档的主体，qa 卡片的详情部分）
     */
    private String contentMd;
    /**
     * qa 结构化字段 JSON：errorText/cause/steps/tips/images/keywords
     */
    @TableField("content_json")
    private String contentJson;
    /**
     * 标签，英文逗号分隔，用于筛选
     */
    private String tags;
    /**
     * 状态：ACTIVE 正常 / DELETED 逻辑删除（DRAFT 草稿预留）
     */
    private String status;
    /**
     * 来源类型：manual 手工新建 / import 批量导入 / migrate 旧库迁移
     */
    private String sourceType;
    /**
     * 来源侧 ID，与 sourceType 组成唯一键，保证迁移幂等
     */
    private String sourceId;
    /**
     * 创建人用户名
     */
    private String createdBy;
    /**
     * 最后修改人用户名
     */
    private String updatedBy;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 最后修改时间
     */
    private Date updatedAt;
}
