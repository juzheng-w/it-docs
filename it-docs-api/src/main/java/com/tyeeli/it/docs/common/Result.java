package com.tyeeli.it.docs.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import io.swagger.annotations.ApiModel;

/**
 * 统一返回体：{code, msg, data}
 * code = 0 成功；非 0 业务失败；HTTP 状态码仍按语义返回（401/403/404/500）
 */
@Data
@ApiModel(value = "Result", description = "统一返回体：code=0 成功，非 0 为业务错误码；HTTP 状态码仍按语义返回")
public class Result<T> {

    public static final int OK = 0;

    @ApiModelProperty(value = "业务码：0 成功，非 0 失败（与 HTTP 状态码并行）", example = "0")
    private int code;
    @ApiModelProperty(value = "提示信息，成功为 success", example = "success")
    private String msg;
    @ApiModelProperty(value = "业务数据，失败时为 null")
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<T>();
        r.code = OK;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<T>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
