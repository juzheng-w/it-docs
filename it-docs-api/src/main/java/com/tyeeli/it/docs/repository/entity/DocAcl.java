package com.tyeeli.it.docs.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 单篇文档读取限制；无授权行时继承空间读取权限。 */
@Data
@TableName("it_doc_acl")
public class DocAcl {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private String subjectType;
    private String subjectName;
    private String createdBy;
    private Date createdAt;
}
