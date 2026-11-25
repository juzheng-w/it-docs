package com.tyeeli.it.docs.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 上传结果：正文里只存 url，不存物理路径
 */
@Data
@ApiModel(value = "FileDTO", description = "上传结果：正文里只存 url，不存物理路径")
public class FileDTO {

    @ApiModelProperty(value = "文件 ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "访问地址，正文里只存这个", example = "/api/files/1/view")
    private String url;
    @ApiModelProperty(value = "原始文件名", example = "入库单截图.png")
    private String name;
    @ApiModelProperty(value = "字节数", example = "204800")
    private Long size;
    @ApiModelProperty(value = "业务类型", example = "attachment")
    private String bizType;
}
