package com.tyeeli.it.docs.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import io.swagger.annotations.ApiModel;

import java.util.List;

/**
 * 分页返回体（列表接口默认 20 / 上限 100）
 */
@Data
@ApiModel(value = "PageResult", description = "分页返回体：total 总数 / page 当前页 / size 每页条数 / list 数据")
public class PageResult<T> {

    @ApiModelProperty(value = "总条数", example = "128")
    private long total;
    @ApiModelProperty(value = "当前页，从 1 开始", example = "1")
    private long page;
    @ApiModelProperty(value = "每页条数", example = "20")
    private long size;
    @ApiModelProperty(value = "当前页数据")
    private List<T> list;

    public static <T> PageResult<T> of(long total, long page, long size, List<T> list) {
        PageResult<T> p = new PageResult<T>();
        p.total = total;
        p.page = page;
        p.size = size;
        p.list = list;
        return p;
    }
}
