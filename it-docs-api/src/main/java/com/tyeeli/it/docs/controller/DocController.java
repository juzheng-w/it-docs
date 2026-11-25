package com.tyeeli.it.docs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.AuthContext;
import com.tyeeli.it.docs.converter.DocConverter;
import com.tyeeli.it.docs.dto.*;
import com.tyeeli.it.docs.repository.entity.Doc;
import com.tyeeli.it.docs.repository.entity.DocVersion;
import com.tyeeli.it.docs.repository.entity.DocAcl;
import com.tyeeli.it.docs.service.DocService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 文档资源（RESTful）：
 * GET    /api/docs                    列表（公共空间免登录，手机端只调读接口）
 * GET    /api/docs/{id}               详情
 * POST   /api/docs                    新建
 * PUT    /api/docs/{id}               全量更新
 * DELETE /api/docs/{id}               软删
 * PUT    /api/docs/{id}/space         换空间（个人区域 → 公共空间即发布）
 * GET    /api/docs/{id}/versions      版本列表
 * GET    /api/docs/{id}/versions/{no} 版本详情（含正文）
 * POST   /api/docs/{id}/versions/{no}/restore  回滚到指定版本
 * <p>
 * 认证由拦截器保证；细粒度空间权限（ACL / 个人区域归属）在 Service 判定，Controller 不重复写。
 */
@Api(tags = "03. 文档", description = "文档的读写、检索、版本与换空间。列表与详情免登录，写操作按空间权限判定")
@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocController {

    private final DocService docService;

    @ApiOperation(value = "文档列表 / 全文检索",
            notes = "不传 spaceId 时只返回「当前用户可见」的文档：可见公共空间（PUBLIC 匿名 / INTERNAL 登录 / "
                    + "RESTRICTED 授权名单）+ 本人个人区域（未登录则仅 PUBLIC 空间）。keyword 有值走 ngram 全文索引。"
                    + "分页默认 20、上限 100。")
        @GetMapping
    public Result<PageResult<DocDTO>> list( DocQuery query) {
        AuthContext.LoginUser u = AuthContext.get();
        PageResult<Doc> page = docService.list(query,
                u == null ? null : u.username, u == null ? null : u.role, u != null && u.ldapUser);
        return Result.ok(PageResult.of(page.getTotal(), page.getPage(), page.getSize(),
                DocConverter.toDTOList(page.getList())));
    }

    @ApiOperation(value = "文档详情",
            notes = "返回含正文的完整内容。个人区域文档仅本人可读（ADMIN 也不越权）。\n\n"
                    + "失败：403 个人区域文档非本人；404 文档不存在、已删除或已下架（下架的仅 ADMIN/EDITOR 可取）。")
        @GetMapping("/{id}")
    public Result<DocDetailDTO> detail(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id) {
        AuthContext.LoginUser u = AuthContext.get();
        return Result.ok(DocConverter.toDetailDTO(docService.detail(id,
                u == null ? null : u.username, u == null ? null : u.role, u != null && u.ldapUser)));
    }

    @ApiOperation(value = "新建文档",
            notes = "新建到个人区域或有权写入的公共空间。标题必填；docType 取 qa（问题记录）、req（业务文档）、tech（技术文档）；md 仅兼容旧数据。"
                    + "docNo、状态、创建人由服务端生成，请求体里传了也无效。\n\n"
                    + "失败：400 参数校验不通过；403 无权写入目标空间。")
    @PostMapping
    public Result<DocDetailDTO> create(@Valid @RequestBody DocSaveRequest req) {
        AuthContext.LoginUser u = AuthContext.require();
        Doc created = docService.create(DocConverter.toEntity(req), u.username, u.role, u.ldapUser);
        return Result.ok(DocConverter.toDetailDTO(created));
    }

    @ApiOperation(value = "更新文档",
            notes = "全量覆盖正文与元数据，每次保存生成一个版本快照。换空间请用 PUT /api/docs/{id}/space。\n\n"
                    + "失败：403 无权修改；404 文档不存在。")
    @PutMapping("/{id}")
    public Result<DocDetailDTO> update(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody DocSaveRequest req) {
        AuthContext.LoginUser u = AuthContext.require();
        Doc updated = docService.update(id, DocConverter.toEntity(req), u.username, u.role, u.ldapUser);
        return Result.ok(DocConverter.toDetailDTO(updated));
    }

    @ApiOperation(value = "删除文档",
            notes = "逻辑删除（status=deleted），删除前快照写入操作日志，可由管理员追溯。\n\n"
                    + "失败：403 无权删除。")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id) {
        AuthContext.LoginUser u = AuthContext.require();
        docService.delete(id, u.username, u.role, u.ldapUser);
        return Result.ok();
    }

    /**
     * 下架 / 恢复：不删数据，只是状态翻转
     */
    @ApiOperation(value = "下架 / 恢复文档",
            notes = "status=DISABLED 下架：查阅端列表立即不出现，直接链接也打不开（前台按 404 处理），"
                    + "工作台仍可见可编辑、随时可恢复；status=ACTIVE 恢复上架。"
                    + "内容、版本、审计全部保留，与删除不同。\n\n"
                    + "失败：400 状态不合法；403 无权修改该空间文档；404 文档不存在。")
    @PatchMapping("/{id}/status")
    public Result<DocDetailDTO> setStatus(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody DocStatusRequest req) {
        AuthContext.LoginUser u = AuthContext.require();
        Doc updated = docService.setStatus(id, req.getStatus(), u.username, u.role, u.ldapUser);
        return Result.ok(DocConverter.toDetailDTO(updated));
    }

    /**
     * 换空间：个人区域 → 公共空间即「发布」
     */
    @ApiOperation(value = "文档换空间（发布）",
            notes = "把文档移到目标空间。个人区域 → 公共空间即视为「发布」；"
                    + "反向移动要求对原空间有写权限且是文档作者。\n\n"
                    + "失败：403 无权移动或目标空间不可写。")
    @PutMapping("/{id}/space")
    public Result<DocDetailDTO> moveToSpace(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody MoveDocRequest req) {
        AuthContext.LoginUser u = AuthContext.require();
        Doc moved = docService.moveToSpace(id, req.getSpaceId(), u.username, u.role, u.ldapUser);
        return Result.ok(DocConverter.toDetailDTO(moved));
    }

    @ApiOperation(value = "版本列表", notes = "按版本号倒序返回快照元信息（不含正文）。")
        @GetMapping("/{id}/versions")
    public Result<List<DocVersionDTO>> versions(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id) {
        AuthContext.LoginUser u = AuthContext.get();
        List<DocVersion> list = docService.versions(id,
                u == null ? null : u.username, u == null ? null : u.role, u != null && u.ldapUser);
        return Result.ok(DocConverter.toVersionDTOList(list));
    }

    @ApiOperation(value = "历史版本详情",
            notes = "返回指定版本号对应的正文快照，用于回滚前预览。版本列表接口不返回正文，避免一次拉取全部内容。\n\n"
                    + "失败：403 无权读取；404 文档或版本号不存在。")
    @GetMapping("/{id}/versions/{no}")
    public Result<DocVersionDetailDTO> version(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id,
            @ApiParam(value = "版本号", required = true, example = "3") @PathVariable int no) {
        AuthContext.LoginUser u = AuthContext.get();
        return Result.ok(DocConverter.toVersionDetailDTO(docService.version(id, no,
                u == null ? null : u.username, u == null ? null : u.role, u != null && u.ldapUser)));
    }

    @ApiOperation(value = "回滚到指定版本",
            notes = "用历史快照覆盖当前内容：标题、摘要、归属系统、分类、处理角色、标签与正文一并恢复；"
                    + "回滚本身也会生成一个新版本，历史不丢。旧快照（snapshot_v=1）只有标题与正文，"
                    + "回滚时不会清空其余字段。\n\n"
                    + "失败：403 无权修改；404 文档或版本号不存在。")
    @PostMapping("/{id}/versions/{no}/restore")
    public Result<DocDetailDTO> restore(
            @ApiParam(value = "文档 ID", required = true, example = "1") @PathVariable Long id,
            @ApiParam(value = "目标版本号", required = true, example = "3") @PathVariable int no) {
        AuthContext.LoginUser u = AuthContext.require();
        Doc restored = docService.restore(id, no, u.username, u.role, u.ldapUser);
        return Result.ok(DocConverter.toDetailDTO(restored));
    }

    @ApiOperation(value = "单篇文档授权列表", notes = "仅该文档的编辑者可查看；为空表示继承空间权限。")
    @GetMapping("/{id}/acl")
    public Result<List<DocAcl>> acl(@PathVariable Long id) {
        AuthContext.LoginUser u = AuthContext.require();
        return Result.ok(docService.listAcl(id, u.username, u.role, u.ldapUser));
    }

    @ApiOperation(value = "保存单篇文档读取限制", notes = "全量覆盖授权；至少保留一条授权才会收紧到名单，清空即继承空间权限。")
    @PutMapping("/{id}/acl")
    public Result<Void> saveAcl(@PathVariable Long id, @Valid @RequestBody List<DocAclRequest> acls) {
        AuthContext.LoginUser u = AuthContext.require();
        docService.saveAcl(id, acls, u.username, u.role, u.ldapUser);
        return Result.ok();
    }
}
