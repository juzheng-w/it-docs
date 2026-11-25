package com.tyeeli.it.docs.controller;

import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.AuthContext;
import com.tyeeli.it.docs.config.RequireRole;
import com.tyeeli.it.docs.dto.DocImportResultDTO;
import com.tyeeli.it.docs.dto.DocQuery;
import com.tyeeli.it.docs.service.DocIoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 文档批量导入导出（CSV，UTF-8 BOM，Excel 可直接打开）。
 * 仅 ADMIN / EDITOR；匿名虽能 GET /api/docs/**，但这里的 @RequireRole 会挡住。
 */
@Api(tags = "07. 文档导入导出", description = "CSV 批量导出/导入/模板；仅 ADMIN/EDITOR")
@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocIoController {

    private final DocIoService docIoService;

    @ApiOperation(value = "导出文档 CSV",
            notes = "沿用列表筛选条件（keyword/spaceId/docType/system/category）导出全量，最多 5000 条；"
                    + "含 Markdown 正文与问答卡结构化 JSON，可再原样导入。UTF-8 BOM，Excel 直接打开不乱码。\n\n"
                    + "失败：401 未登录；403 VIEWER 无权限。")
    @GetMapping("/export")
    @RequireRole({Constants.ROLE_ADMIN, Constants.ROLE_EDITOR})
    public void export(DocQuery query, HttpServletResponse response) throws IOException {
        AuthContext.LoginUser u = AuthContext.require();
        String csv = docIoService.exportCsv(query, u.username, u.role, u.ldapUser);
        writeCsv(response, csv, "it-docs-" + LocalDate.now().toString().replace("-", "") + ".csv");
    }

    @ApiOperation(value = "下载导入模板 CSV", notes = "表头 + 一行示例；按表头名匹配，列顺序可任意。")
    @GetMapping("/export/template")
    @RequireRole({Constants.ROLE_ADMIN, Constants.ROLE_EDITOR})
    public void template(HttpServletResponse response) throws IOException {
        writeCsv(response, docIoService.templateCsv(), "it-docs-import-template.csv");
    }

    @ApiOperation(value = "导入文档 CSV",
            notes = "逐行建档/更新，单行失败不中断；按 docNo 优先、同空间同标题兜底判重。"
                    + "updateExisting=false 时重复行跳过，true 时更新已有文档（生成新版本快照 + 审计日志）。\n\n"
                    + "失败：400 文件为空/缺 title 列/超 10MB；403 VIEWER 无权限。")
    @PostMapping("/import")
    @RequireRole({Constants.ROLE_ADMIN, Constants.ROLE_EDITOR})
    public Result<DocImportResultDTO> importCsv(
            @ApiParam(value = "CSV 文件", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "默认目标空间 ID（行内 spaceId 为空时用）", example = "1")
            @RequestParam(value = "spaceId", required = false) Long spaceId,
            @ApiParam(value = "已存在时更新（false=跳过）", example = "false")
            @RequestParam(value = "updateExisting", required = false, defaultValue = "false") boolean updateExisting) {
        AuthContext.LoginUser u = AuthContext.require();
        try {
            DocImportResultDTO r = docIoService.importCsv(file.getBytes(), spaceId, updateExisting,
                    u.username, u.role, u.ldapUser);
            return Result.ok(r);
        } catch (IOException e) {
            throw com.tyeeli.it.docs.common.BizException.badRequest("读取上传文件失败: " + e.getMessage());
        }
    }

    private void writeCsv(HttpServletResponse response, String csv, String filename) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}
