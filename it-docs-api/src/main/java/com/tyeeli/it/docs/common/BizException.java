package com.tyeeli.it.docs.common;

/**
 * 业务异常：全局异常处理器统一转 Result。
 * code 约定：401 未登录 / 403 无权限 / 404 不存在 / 400 参数错 / 500 内部错
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }

    public static BizException unauthorized(String msg) {
        return new BizException(401, msg);
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }

    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    public int getCode() {
        return code;
    }
}
