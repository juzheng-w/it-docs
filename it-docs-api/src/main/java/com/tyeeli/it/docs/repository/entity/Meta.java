package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 基础数据字典（it_meta）：业务系统 / 问题分类，带颜色。
 * 文档表里存的是 name 字符串（biz_system/category），字典只管下拉选项与展示色，
 * 删字典项不影响历史文档。标签不入字典——标签直接写在文档 tags 字段里。
 */
@Data
@TableName("it_meta")
public class Meta {

    /**
     * 字典类型：BIZ_SYSTEM / CATEGORY
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    private String metaType;

    /**
     * 父级：仅 CATEGORY 用，绑定 BIZ_SYSTEM 字典项 id（该分类只出现在这个系统的下拉里）；
     * 空 = 通用分类（任何系统可选）。BIZ_SYSTEM 恒为空。
     */
    private Long parentId;

    /**
     * 字典项名称，同一类型内唯一；文档里存的就是它
     */
    private String name;

    /**
     * 展示色 #RRGGBB，空则前端用默认色
     */
    private String color;

    /**
     * 排序号，小的在前
     */
    private Integer sortNo;

    private String status;

    private String createdBy;

    private Date createdAt;

    private Date updatedAt;
}
