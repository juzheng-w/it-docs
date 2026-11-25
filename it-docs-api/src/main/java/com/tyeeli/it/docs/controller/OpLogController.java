package com.tyeeli.it.docs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.RequireRole;
import com.tyeeli.it.docs.converter.OpLogConverter;
import com.tyeeli.it.docs.dto.OpLogDTO;
import com.tyeeli.it.docs.dto.OpLogQuery;
import com.tyeeli.it.docs.repository.entity.OpLog;
import com.tyeeli.it.docs.service.OpLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志（只读，仅 ADMIN）：GET /api/op-logs?docId=&operator=&page=&size=
 */
@Api(tags = "06. 操作日志", description = "文档变更审计，含改前改后快照。整个控制器仅 ADMIN 可访问")
@RestController
@RequestMapping("/api/op-logs")
@RequiredArgsConstructor
@RequireRole(Constants.ROLE_ADMIN)
public class OpLogController {

    private final OpLogService opLogService;

    @ApiOperation(value = "操作日志分页查询",
            notes = "按文档 ID、操作人过滤，按操作时间倒序。beforeJson / afterJson 为改前改后快照，用于追溯与回滚判断。\n\n"
                    + "失败：403 非管理员。")
    @GetMapping
    public Result<PageResult<OpLogDTO>> list( OpLogQuery query) {
        PageResult<OpLog> page = opLogService.page(query);
        return Result.ok(PageResult.of(page.getTotal(), page.getPage(), page.getSize(),
                OpLogConverter.toDTOList(page.getList())));
    }
}
