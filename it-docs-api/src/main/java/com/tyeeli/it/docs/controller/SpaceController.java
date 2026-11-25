package com.tyeeli.it.docs.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.AuthContext;
import com.tyeeli.it.docs.config.RequireRole;
import com.tyeeli.it.docs.converter.SpaceConverter;
import com.tyeeli.it.docs.dto.*;
import com.tyeeli.it.docs.repository.entity.Space;
import com.tyeeli.it.docs.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 空间资源 + 授权子资源：
 * GET    /api/spaces?scope=public|all     空间列表（默认=当前用户可见；public 匿名=仅 PUBLIC；all 需 ADMIN）
 * POST   /api/spaces                      新建公共空间（visibility 三档）
 * PUT    /api/spaces/{id}                 更新空间信息（含可见性）
 * PATCH  /api/spaces/{id}/status          停用 / 恢复
 * GET    /api/spaces/{id}/acl             授权列表
 * PUT    /api/spaces/{id}/acl             全量覆盖授权
 */
@Api(tags = "02. 空间与授权", description = "公共空间（可见性三档）与个人区域。管理操作限 ADMIN，列表按可见性过滤")
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    /**
     * 默认（不带 scope）：匿名=仅 PUBLIC 可见空间；登录=PUBLIC+INTERNAL+已授权的 RESTRICTED。
     * scope=all：管理视角全部空间（含停用与个人区域），仅 ADMIN。
     */
    @ApiOperation(value = "空间列表",
            notes = "默认按当前登录态过滤：匿名仅 visibility=PUBLIC 的空间；登录用户额外可见 INTERNAL，"
                    + "RESTRICTED 空间需 ACL 授权（ADMIN 直通）。\n\n"
                    + "scope=all 返回全部空间（含停用、含个人区域），仅 ADMIN。\n\n"
                    + "失败：403 scope=all 但当前用户不是管理员。")
    @GetMapping
    public Result<List<SpaceDTO>> list(
            @ApiParam(value = "默认按登录态过滤；all=管理视角全部空间", allowableValues = "all",
                    example = "all")
            @RequestParam(required = false) String scope) {
        boolean all = "all".equalsIgnoreCase(scope);
        if (all) {
            if (!AuthContext.isAdmin()) {
                throw com.tyeeli.it.docs.common.BizException.forbidden("仅管理员可查看全部空间");
            }
            return Result.ok(SpaceConverter.toDTOList(spaceService.listAll()));
        }
        AuthContext.LoginUser u = AuthContext.get();
        List<Space> list = u == null
                ? spaceService.listPublic()
                : spaceService.listVisible(u.username, u.role, u.ldapUser);
        return Result.ok(SpaceConverter.toDTOList(list));
    }

    @ApiOperation(value = "新建公共空间",
            notes = "编码（code）全局唯一，创建后不可修改。visibility：PUBLIC 匿名可看 / INTERNAL 登录可看"
                    + "（默认）/ RESTRICTED 仅 ACL 名单。\n\n"
                    + "失败：400 名称或编码不合规、编码重复、visibility 不合法；403 非管理员。")
    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<SpaceDTO> create(
            @Valid @RequestBody SpaceCreateRequest req) {
        return Result.ok(SpaceConverter.toDTO(spaceService.create(req, AuthContext.require().username)));
    }

    @ApiOperation(value = "更新空间信息",
            notes = "改名称 / 描述 / 可见性，编码不可变。RESTRICTED 名单在 PUT /{id}/acl 维护。\n\n"
                    + "失败：400 参数不合规；403 非管理员；404 空间不存在。")
    @PutMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> update(
            @ApiParam(value = "空间 ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody SpaceUpdateRequest req) {
        spaceService.update(id, req);
        return Result.ok();
    }

    /**
     * 停用 / 恢复：停用后所有列表不出现（空间管理页除外），文档数据全保留
     */
    @ApiOperation(value = "停用 / 恢复空间",
            notes = "status=DISABLED 停用：查阅端与工作台立即不可见，空间管理页仍可见可恢复；"
                    + "数据与文档全保留，与删除不同。个人区域不能停用。\n\n"
                    + "失败：400 status 不合法或个人区域停用；403 非管理员；404 空间不存在。")
    @PatchMapping("/{id}/status")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> changeStatus(
            @ApiParam(value = "空间 ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody SpaceStatusRequest req) {
        spaceService.changeStatus(id, req.getStatus(), AuthContext.require().username);
        return Result.ok();
    }

    @ApiOperation(value = "空间授权列表",
            notes = "返回该空间全部 USER / LDAP_GROUP 授权项。\n\n失败：403 非管理员。")
    @GetMapping("/{id}/acl")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<List<AclDTO>> acl(
            @ApiParam(value = "空间 ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(SpaceConverter.toAclDTOList(spaceService.listAcl(id)));
    }

    /**
     * 全量覆盖式保存（前端提交整个数组）
     */
    @ApiOperation(value = "保存空间授权（全量覆盖）",
            notes = "提交整个数组即为最终状态，未出现的授权项会被删除。"
                    + "role 只接受 EDITOR / VIEWER；ADMIN 全局可写，无需逐空间授权。\n\n"
                    + "失败：400 主体类型或角色取值不合法；403 非管理员。")
    @PutMapping("/{id}/acl")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> saveAcl(
            @ApiParam(value = "空间 ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody List<AclItemRequest> acls) {
        spaceService.saveAcl(id, acls, AuthContext.require().username);
        return Result.ok();
    }
}
