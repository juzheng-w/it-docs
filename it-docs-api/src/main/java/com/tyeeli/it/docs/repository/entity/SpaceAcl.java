package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 空间授权表：公共空间按主体（用户 / 域组）授予读写权限。
 * 个人空间不走 ACL，仅 owner 可读写。
 */
@Data
@TableName("it_space_acl")
public class SpaceAcl {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 空间 ID，指向 it_space
     */
    private Long spaceId;
    /**
     * 授权主体类型：USER 用户 / LDAP_GROUP 域组（二期启用）
     */
    private String subjectType;
    /**
     * 主体标识：用户名 或 域组 CN
     */
    private String subjectName;
    /**
     * 空间内角色：EDITOR 可写 / VIEWER 只读
     */
    private String role;
    /**
     * 授权操作人用户名
     */
    private String createdBy;
    /**
     * 授权时间
     */
    private Date createdAt;
}
