package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单条提示信息（如 LDAP 测试结果）
 */
@Data
@AllArgsConstructor
@ApiModel(value = "MessageDTO", description = "单条提示信息（如 LDAP 测试结果、临时密码）")
public class MessageDTO {

    @ApiModelProperty(value = "提示信息，如「连接成功」或临时密码", example = "连接成功")
    private String message;
}
