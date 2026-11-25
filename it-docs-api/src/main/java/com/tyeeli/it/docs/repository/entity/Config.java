package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 系统配置表（键值对）：LDAP 连接等界面可改的配置存这里。
 * 读取优先级 DB &gt; 环境变量 &gt; 默认值，改完即时生效，无需重启。
 */
@Data
@TableName("it_config")
public class Config {

    /**
     * 配置键（主键），如 ldap.url、ldap.base-dn
     */
    @TableId(value = "cfg_key", type = IdType.INPUT)
    private String cfgKey;

    /**
     * 配置值，明文存储（含 LDAP 绑定密码，库权限需收紧）
     */
    private String cfgValue;

    /**
     * 备注，说明该项用途
     */
    private String remark;

    /**
     * 最后修改人用户名
     */
    private String updatedBy;

    /**
     * 最后修改时间
     */
    private Date updatedAt;
}
