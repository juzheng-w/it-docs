package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 空间表：PUBLIC 为团队公共区域，PERSONAL 为每人一个的个人区域。
 * 个人区域编码固定 P-&lt;username&gt;，由服务端幂等创建。
 */
@Data
@TableName("it_space")
public class Space {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 空间名称，界面展示用
     */
    private String spaceName;
    /**
     * 空间编码，唯一；个人区域固定为 P-&lt;username&gt;
     */
    private String spaceCode;
    /**
     * 空间类型：PUBLIC 公共 / PERSONAL 个人
     */
    private String spaceType;
    /**
     * 可见性（仅公共空间用）：PUBLIC 匿名可看 / INTERNAL 登录可看 / RESTRICTED 仅 ACL 授权名单；
     * 个人空间不看这一列，恒走 owner 校验
     */
    private String visibility;
    /**
     * 空间用途说明
     */
    private String description;
    /**
     * PERSONAL 空间的归属用户名；PUBLIC 为空
     */
    private String owner;
    /**
     * 来源类型：manual 手工新建 / import 导入 / migrate 迁移
     */
    private String sourceType;
    /**
     * 来源侧 ID，配合 sourceType 保证迁移幂等
     */
    private String sourceId;
    /**
     * 状态：ACTIVE 正常 / DELETED 逻辑删除
     */
    private String status;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 最后修改时间
     */
    private Date updatedAt;
}
