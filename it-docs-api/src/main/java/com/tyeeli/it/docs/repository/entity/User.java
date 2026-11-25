package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户表：本地账号与域账号双源，登录时按 authSource 分流校验。
 * 停用只改 status，不删数据（文档与操作日志需要保留归属）。
 */
@Data
@TableName("it_user")
public class User {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 登录名，唯一；LDAP 模式下与域账号一致
     */
    private String username;
    /**
     * 显示名，界面与日志展示用
     */
    private String displayName;
    /**
     * 认证来源：LDAP 域认证 / LOCAL 本地密码
     */
    private String authSource;
    /**
     * 本地密码的 BCrypt 哈希；LDAP 用户为空
     */
    private String passwordHash;
    /**
     * 全局角色：ADMIN 管理员 / EDITOR 可写 / VIEWER 只读
     */
    private String role;
    /**
     * 数据权限：可见业务系统（逗号分隔，如 SVS,OTWB）；空 = 全部系统。
     * ADMIN 不受限制；限制后仅能看/改归属这些系统的问答卡，无系统归属的文档仍可见
     */
    private String sysScope;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 账号状态：ACTIVE 正常 / DISABLED 停用（拦截器校验）
     */
    private String status;
    /**
     * 是否强制改密：1 是 / 0 否；首登与管理员重置后置 1
     */
    private Integer mustChangePwd;
    /**
     * 令牌版本：改密 / 管理员重置后自增，签发更早的 JWT 立即失效
     */
    private Integer tokenVersion;
    /**
     * 最后登录时间
     */
    private Date lastLoginAt;
    /**
     * 创建人用户名
     */
    private String createdBy;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 最后修改时间
     */
    private Date updatedAt;

    /**
     * 临时明文密码：仅在创建 / 重置的响应里返回一次，不落库
     */
    @TableField(exist = false)
    private transient String tempPassword;
}
