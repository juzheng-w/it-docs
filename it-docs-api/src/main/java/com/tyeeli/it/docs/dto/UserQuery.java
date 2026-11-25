package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.tyeeli.it.docs.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成员列表查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "UserQuery", description = "成员列表查询条件（GET query 参数）")
public class UserQuery extends PageQuery {

    /**
     * 用户名 / 姓名模糊
     */
    @ApiModelProperty(value = "用户名或姓名模糊匹配", example = "zhang")
    private String keyword;

    /**
     * LDAP / LOCAL
     */
    @ApiModelProperty(value = "账号来源：LOCAL 本地账号 / LDAP 域账号", example = "LOCAL")
    private String authSource;
}
