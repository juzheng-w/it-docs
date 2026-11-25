package com.tyeeli.it.docs.controller;

import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.Result;
import com.tyeeli.it.docs.config.AuthContext;
import com.tyeeli.it.docs.config.RequireRole;
import com.tyeeli.it.docs.dto.MetaDTO;
import com.tyeeli.it.docs.dto.MetaSaveRequest;
import com.tyeeli.it.docs.service.MetaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 基础数据字典（业务系统/问题分类，带颜色）。
 * 列表任何登录用户可读（编辑表单下拉用）；增删改仅 ADMIN。
 * 标签不在这里——标签直接写在文档 tags 字段里。
 */
@Api(tags = "06. 基础数据字典", description = "业务系统/问题分类字典；读登录即可，写限 ADMIN（标签不是字典）")
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    /**
     * 字典列表（公开）：编辑表单下拉、查阅端筛选/彩色标签用。
     * 匿名可读——字典只有名称与颜色，无敏感信息；写操作仍仅 ADMIN。
     */
    @ApiOperation(value = "字典列表（公开）",
            notes = "type=BIZ_SYSTEM（业务系统）/ CATEGORY（问题分类）；"
                    + "不传 type 返回全部。按 sortNo 升序。\n\n"
                    + "失败：400 type 不合法。")
    @GetMapping
    public Result<List<MetaDTO>> list(
            @ApiParam(value = "字典类型，空则全部", example = "CATEGORY")
            @RequestParam(required = false) String type) {
        return Result.ok(metaService.list(type));
    }

    @ApiOperation(value = "新增字典项",
            notes = "type 必须是 BIZ_SYSTEM/CATEGORY；同类型名称唯一；"
                    + "color 为 #RRGGBB 可空。\n\n失败：400 参数不合规或重名；403 非管理员。")
    @PostMapping
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<MetaDTO> create(@Valid @RequestBody MetaSaveRequest req) {
        return Result.ok(metaService.create(req, AuthContext.require().username));
    }

    @ApiOperation(value = "编辑字典项",
            notes = "改名称/颜色/排序号；类型不可改；同类型名称唯一。\n\n"
                    + "失败：400 参数不合规或重名；404 不存在；403 非管理员。")
    @PutMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<MetaDTO> update(
            @ApiParam(value = "字典项 ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody MetaSaveRequest req) {
        return Result.ok(metaService.update(id, req, AuthContext.require().username));
    }

    @ApiOperation(value = "删除字典项",
            notes = "物理删除。历史文档存的是名称字符串，不受影响（只是不再出现在下拉里）。\n\n"
                    + "失败：404 不存在；403 非管理员。")
    @DeleteMapping("/{id}")
    @RequireRole(Constants.ROLE_ADMIN)
    public Result<Void> delete(
            @ApiParam(value = "字典项 ID", required = true, example = "1") @PathVariable Long id) {
        metaService.delete(id);
        return Result.ok(null);
    }
}
