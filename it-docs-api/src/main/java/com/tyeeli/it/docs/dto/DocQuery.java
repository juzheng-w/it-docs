package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.tyeeli.it.docs.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档列表查询条件（GET query 参数绑定）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "DocQuery", description = "文档列表查询条件（GET query 参数）")
public class DocQuery extends PageQuery {

    @ApiModelProperty(value = "空间 ID，为空表示「当前用户可见范围」（公共空间 + 本人个人区域）", example = "1")
    private Long spaceId;

    @ApiModelProperty(value = "归属系统，如 WMS / ERP / SRM", example = "WMS")
    private String system;

    @ApiModelProperty(value = "文档分类", example = "采购入库")
    private String category;

    /**
     * qa / req / tech / md（旧版）
     */
    @ApiModelProperty(value = "文档类型：qa 问题记录 / req 业务文档 / tech 技术文档 / md 旧版文档", example = "tech")
    private String docType;

    /**
     * 有值走 FULLTEXT ngram 全文检索
     */
    @ApiModelProperty(value = "全文检索关键字（ngram），命中标题与正文；有值时走全文索引", example = "入库单")
    private String keyword;

    /**
     * 工作台要看到已下架的文档才能恢复，查阅端不传（默认只出在架文档）
     */
    @ApiModelProperty(value = "是否包含已下架文档（工作台传 true；查阅端不传，只看在架）", example = "false")
    private Boolean includeDisabled;
}
