package com.tyeeli.it.docs.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 分页查询基类：所有列表 Query DTO 继承它，统一 page/size 口径（默认 20，上限 100）。
 * 前端传 page<1 或 size 越界时静默纠正，不报错（避免手机端异常参数打 400）。
 */
@Data
@ApiModel(value = "PageQuery", description = "分页查询基类：page 默认 1，size 默认 20、上限 100，越界静默纠正不报 400")
public class PageQuery {

    @ApiModelProperty(value = "页码，从 1 开始；小于 1 静默纠正为 1", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页条数，默认 20，上限 100", example = "20")
    private Integer size = 20;

    public int pageNo() {
        return page == null || page < 1 ? 1 : page;
    }

    public int pageSize() {
        if (size == null || size < 1) {
            return 20;
        }
        return size > 100 ? 100 : size;
    }
}
