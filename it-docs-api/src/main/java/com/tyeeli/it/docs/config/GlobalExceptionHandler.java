package com.tyeeli.it.docs.config;

import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;

/**
 * 全局异常出口（Controller 层不做 try-catch）：
 * - BizException：业务异常，按自带 code 转 HTTP 状态
 * - 参数校验失败（@Valid）：400 + 首条字段提示
 * - 未知异常：500，日志留堆栈，响应不外泄
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> biz(BizException e) {
        HttpStatus hs;
        try {
            hs = HttpStatus.valueOf(e.getCode());
        } catch (Exception ignore) {
            hs = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(hs).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * @RequestBody + @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> invalidBody(MethodArgumentNotValidException e) {
        return badRequest(firstError(e.getBindingResult().getFieldErrors()));
    }

    /**
     * 表单 / query 参数绑定 + 校验失败
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> invalidBind(BindException e) {
        return badRequest(firstError(e.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> invalidParam(ConstraintViolationException e) {
        String msg = "请求参数不合法";
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            msg = v.getMessage();
            break;
        }
        return badRequest(msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> unreadable(HttpMessageNotReadableException e) {
        return badRequest("请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> missingParam(MissingServletRequestParameterException e) {
        return badRequest("缺少参数：" + e.getParameterName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> uploadTooLarge(MaxUploadSizeExceededException e) {
        return badRequest("上传文件过大（单文件上限 20 MB）");
    }

    /**
     * 服务级前置条件不满足（如 LDAP 未配置/服务不可用）：503 + 具体提示
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> unavailable(IllegalStateException e) {
        log.warn("服务暂不可用: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.fail(503, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unknown(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(500).body(Result.fail(500, "服务异常，请稍后重试"));
    }

    private ResponseEntity<Result<Void>> badRequest(String msg) {
        return ResponseEntity.badRequest().body(Result.fail(400, msg));
    }

    private String firstError(List<FieldError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "请求参数不合法";
        }
        FieldError fe = errors.get(0);
        return fe.getDefaultMessage();
    }
}
