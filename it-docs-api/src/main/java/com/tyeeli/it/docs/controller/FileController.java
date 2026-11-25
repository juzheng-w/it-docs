package com.tyeeli.it.docs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.AuthContext;
import com.tyeeli.it.docs.converter.FileConverter;
import com.tyeeli.it.docs.dto.FileDTO;
import com.tyeeli.it.docs.repository.entity.FileMeta;
import com.tyeeli.it.docs.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件资源：
 * POST   /api/files                上传（需登录）
 * GET    /api/files/{id}/view      图片内联预览（免登录，但跟随所属文档可读性）
 * GET    /api/files/{id}/download  附件下载（免登录，但跟随所属文档可读性）
 * DELETE /api/files/{id}           逻辑删除（需登录）
 * <p>
 * 两个读接口在拦截器白名单里，鉴权由 FileService#mustReadable 二次完成：
 * 文件挂在个人区文档下时，未登录/非本人一律 403。
 */
@Api(tags = "04. 附件", description = "上传走 multipart/form-data，按文档和上传人范围去重；正文里只保存 url")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    private static String currentUserOrNull() {
        AuthContext.LoginUser u = AuthContext.get();
        return u == null ? null : u.username;
    }

    private static String currentRoleOrNull() {
        AuthContext.LoginUser u = AuthContext.get();
        return u == null ? null : u.role;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static MediaType parse(String mime) {
        try {
            return MediaType.parseMediaType(mime == null ? "application/octet-stream" : mime);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @ApiOperation(value = "上传文件",
            notes = "multipart/form-data 单文件，上限 20MB，类型白名单：png/jpg/gif/webp/svg/pdf/"
                    + "doc/docx/xls/xlsx/ppt/pptx/zip/md/txt。同内容仅在同一文档和上传人范围内复用记录。"
                    + "docId 可空用于新建前临时上传，文档保存后需绑定；未绑定文件仅上传人可读。\n\n"
                    + "失败：400 文件为空或类型不在白名单。")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileDTO> upload(
            @ApiParam(value = "文件本体，≤20MB", required = true)
            @RequestParam("file") MultipartFile file,
            @ApiParam(value = "业务类型，如 doc-cover / attachment", example = "attachment")
            @RequestParam(required = false) String bizType,
            @ApiParam(value = "归属文档 ID，草稿图为空", example = "1")
            @RequestParam(required = false) Long docId) {
        AuthContext.LoginUser u = AuthContext.require();
        return Result.ok(FileConverter.toDTO(fileService.upload(file, bizType, docId,
                u.username, u.role, u.ldapUser)));
    }

    /** 图片内联预览：每次请求都执行文档授权，不能写入共享缓存。 */
    @ApiOperation(value = "图片内联预览",
            notes = "返回原始字节流，带永久强缓存头（内容寻址，同一 id 内容永不变化）。"
                    + "免登录，但文件若挂在个人区文档下，未登录或非本人返回 403。")
        @ApiResponse(code = 200, message = "文件字节流", response = byte[].class)
    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> view(
            @ApiParam(value = "文件 ID", required = true, example = "1") @PathVariable Long id) {
        AuthContext.LoginUser u = AuthContext.get();
        FileMeta f = fileService.mustReadable(id, currentUserOrNull(), currentRoleOrNull(), u != null && u.ldapUser);
        try (InputStream in = fileService.openStream(f)) {
            return ResponseEntity.ok()
                    .contentType(parse(f.getMime()))
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .body(readAll(in));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 附件下载（还原原始文件名）
     */
    @ApiOperation(value = "附件下载",
            notes = "以 attachment 方式返回，文件名按 RFC 5987 编码，中文名不乱码。可读性校验同预览接口。")
        @ApiResponse(code = 200, message = "文件字节流", response = byte[].class)
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @ApiParam(value = "文件 ID", required = true, example = "1") @PathVariable Long id) {
        AuthContext.LoginUser u = AuthContext.get();
        FileMeta f = fileService.mustReadable(id, currentUserOrNull(), currentRoleOrNull(), u != null && u.ldapUser);
        try (InputStream in = fileService.openStream(f)) {
            byte[] body = readAll(in);
            String name = URLEncoder.encode(f.getOriginName(), StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length))
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "删除附件", notes = "逻辑删除元数据，对象存储文件由清理任务统一回收。")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @ApiParam(value = "文件 ID", required = true, example = "1") @PathVariable Long id) {
        AuthContext.LoginUser u = AuthContext.require();
        fileService.softDelete(id, u.username, u.role, u.ldapUser);
        return Result.ok();
    }

    @ApiOperation(value = "将上传中的文件绑定到文档")
    @PatchMapping("/{id}/doc/{docId}")
    public Result<Void> bindToDoc(@PathVariable Long id, @PathVariable Long docId) {
        AuthContext.LoginUser u = AuthContext.require();
        fileService.bindToDoc(id, docId, u.username, u.role, u.ldapUser);
        return Result.ok();
    }
}
