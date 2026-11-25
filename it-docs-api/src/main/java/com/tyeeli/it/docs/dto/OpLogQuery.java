package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.tyeeli.it.docs.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "OpLogQuery", description = "操作日志查询条件（GET query 参数）")
public class OpLogQuery extends PageQuery {

    @ApiModelProperty(value = "文档 ID，按单篇文档追溯", example = "1")
    private Long docId;

    @ApiModelProperty(value = "操作人用户名", example = "admin")
    private String operator;
}
