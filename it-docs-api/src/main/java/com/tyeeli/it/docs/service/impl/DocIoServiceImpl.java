package com.tyeeli.it.docs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.tyeeli.it.docs.common.BizException;
import com.tyeeli.it.docs.common.Constants;
import com.tyeeli.it.docs.common.CsvIo;
import com.tyeeli.it.docs.common.PageResult;
import com.tyeeli.it.docs.converter.DocConverter;
import com.tyeeli.it.docs.dto.DocImportResultDTO;
import com.tyeeli.it.docs.dto.DocQuery;
import com.tyeeli.it.docs.dto.DocSaveRequest;
import com.tyeeli.it.docs.repository.DocRepository;
import com.tyeeli.it.docs.repository.entity.Doc;
import com.tyeeli.it.docs.service.DocIoService;
import com.tyeeli.it.docs.service.DocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入导出实现：导出复用列表查询条件逐页拉取；导入复用 DocService.create/update
 * （连带版本快照、审计日志、空间写权限校验都不绕过）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocIoServiceImpl implements DocIoService {

    private final DocService docService;
    private final DocRepository docRepository;

    private static final int MAX_EXPORT = 5000;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    @Override
    public String exportCsv(DocQuery query, String currentUser, String globalRole, boolean ldapUser) {
        StringBuilder sb = new StringBuilder(CsvIo.BOM);
        sb.append(CsvIo.row(CsvIo.CN_HEADERS)).append('\n');
        query.setSize(100);
        int page = 1;
        int written = 0;
        while (written < MAX_EXPORT) {
            query.setPage(page);
            PageResult<Doc> p = docService.list(query, currentUser, globalRole, ldapUser);
            List<Doc> rows = p.getList();
            if (rows == null || rows.isEmpty()) {
                break;
            }
            for (Doc d : rows) {
                sb.append(CsvIo.row(
                        d.getDocNo(), d.getTitle(), d.getDocType(),
                        d.getSpaceId() == null ? "" : String.valueOf(d.getSpaceId()),
                        d.getSystem(), d.getCategory(), d.getOwner(), d.getSummary(),
                        d.getTags(), d.getContentMd(), d.getContentJson()))
                        .append('\n');
                written++;
                if (written >= MAX_EXPORT) {
                    break;
                }
            }
            if (written >= MAX_EXPORT || rows.size() < 100 || written >= p.getTotal()) {
                break;
            }
            page++;
        }
        return sb.toString();
    }

    @Override
    public String templateCsv() {
        StringBuilder sb = new StringBuilder(CsvIo.BOM);
        sb.append(CsvIo.row(CsvIo.CN_HEADERS)).append('\n');
        sb.append(CsvIo.row(
                "", "示例：WMS 入库单创建报错", "qa", "", "SVS", "接口报错", "运维组",
                "提交入库单提示供应商不存在", "高频,需SQL",
                "## 现象\n提交入库单报错", "{\"errorText\":\"供应商不存在\",\"steps\":[\"核对主数据\",\"重推接口\"]}"))
                .append('\n');
        return sb.toString();
    }

    @Override
    public DocImportResultDTO importCsv(byte[] bytes, Long defaultSpaceId, boolean updateExisting,
                                       String operator, String globalRole, boolean ldapUser) {
        if (bytes == null || bytes.length == 0) {
            throw BizException.badRequest("文件为空");
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw BizException.badRequest("文件超过 10 MB 上限");
        }
        String text = decode(bytes);
        List<String[]> rows = CsvIo.parse(text);
        if (rows.isEmpty()) {
            throw BizException.badRequest("CSV 无内容");
        }

        Map<String, Integer> idx = headerIndex(rows.get(0));
        if (!idx.containsKey("title")) {
            throw BizException.badRequest("缺少「标题」列（表头须含 编号/标题/正文 等中文名，或英文 docNo/title），请先下载模板");
        }

        DocImportResultDTO r = new DocImportResultDTO();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (isEmptyRow(row)) {
                continue;
            }
            r.setTotal(r.getTotal() + 1);
            int line = i + 1;
            try {
                String title = val(row, idx, "title", true);
                if (StrUtil.isBlank(title)) {
                    r.setSkipped(r.getSkipped() + 1);
                    continue;
                }
                Long spaceId = parseSpaceId(val(row, idx, "spaceId", true), defaultSpaceId);
                if (spaceId == null) {
                    throw BizException.badRequest("未指定目标空间（行内 spaceId 为空且未选默认空间）");
                }
                String docNo = val(row, idx, "docNo", true);
                Doc existing = docRepository.findByDocNo(docNo);
                if (existing == null) {
                    existing = docRepository.findActiveByTitleAndSpace(title, spaceId);
                }
                if (existing != null && !updateExisting) {
                    r.setSkipped(r.getSkipped() + 1);
                    continue;
                }

                DocSaveRequest req = new DocSaveRequest();
                req.setSpaceId(spaceId);
                String type = val(row, idx, "docType", true);
                req.setDocType(Constants.DOC_QA.equals(type) ? Constants.DOC_QA : Constants.DOC_MD);
                req.setTitle(title);
                req.setSystem(val(row, idx, "system", true));
                req.setCategory(val(row, idx, "category", true));
                req.setOwner(val(row, idx, "owner", true));
                req.setSummary(val(row, idx, "summary", true));
                req.setTags(val(row, idx, "tags", true));
                // 正文不 trim，保留换行与缩进
                req.setContentMd(val(row, idx, "contentMd", false));
                req.setContentJson(val(row, idx, "contentJson", false));

                Doc input = DocConverter.toEntity(req);
                if (existing != null) {
                    docService.update(existing.getId(), input, operator, globalRole, ldapUser);
                    r.setUpdated(r.getUpdated() + 1);
                } else {
                    docService.create(input, operator, globalRole, ldapUser);
                    r.setCreated(r.getCreated() + 1);
                }
            } catch (Exception e) {
                if (r.getErrors().size() < 20) {
                    r.getErrors().add("第 " + line + " 行：" + rootMessage(e));
                }
            }
        }
        log.info("文档 CSV 导入 operator={} total={} created={} updated={} skipped={} errors={}",
                operator, r.getTotal(), r.getCreated(), r.getUpdated(), r.getSkipped(), r.getErrors().size());
        return r;
    }

    /** 表头 → 列号：支持英文列名与常见中文别名，顺序任意 */
    private Map<String, Integer> headerIndex(String[] header) {
        Map<String, String> alias = new HashMap<String, String>();
        alias.put("编号", "docNo");
        alias.put("文档编号", "docNo");
        alias.put("标题", "title");
        alias.put("类型", "docType");
        alias.put("文档类型", "docType");
        alias.put("空间", "spaceId");
        alias.put("空间id", "spaceId");
        alias.put("系统", "system");
        alias.put("归属系统", "system");
        alias.put("业务系统", "system");
        alias.put("分类", "category");
        alias.put("问题分类", "category");
        alias.put("处理角色", "owner");
        alias.put("责任人", "owner");
        alias.put("摘要", "summary");
        alias.put("一句话答案", "summary");
        alias.put("标签", "tags");
        alias.put("正文", "contentMd");
        alias.put("markdown", "contentMd");
        alias.put("问答结构化", "contentJson");

        Map<String, Integer> idx = new HashMap<String, Integer>();
        for (int i = 0; i < header.length; i++) {
            String h = StrUtil.trimToEmpty(header[i]).toLowerCase();
            if (h.startsWith("\uFEFF")) {
                h = h.substring(1);
            }
            String key = alias.get(h);
            for (String canonical : CsvIo.HEADERS) {
                if (canonical.toLowerCase().equals(h)) {
                    key = canonical;
                    break;
                }
            }
            if (key != null && !idx.containsKey(key)) {
                idx.put(key, i);
            }
        }
        return idx;
    }

    private String val(String[] row, Map<String, Integer> idx, String key, boolean trim) {
        Integer p = idx.get(key);
        if (p == null || p < 0 || p >= row.length) {
            return "";
        }
        String s = row[p] == null ? "" : row[p];
        return trim ? s.trim() : s;
    }

    private boolean isEmptyRow(String[] row) {
        for (String s : row) {
            if (StrUtil.isNotBlank(s)) {
                return false;
            }
        }
        return true;
    }

    private Long parseSpaceId(String raw, Long defaultSpaceId) {
        if (StrUtil.isBlank(raw)) {
            return defaultSpaceId;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw BizException.badRequest("spaceId 不是数字：" + raw);
        }
    }

    /** UTF-8 优先，出现替换符说明是 Excel 另存的 GBK/ANSI → 回退 GBK */
    private String decode(byte[] bytes) {
        String s = new String(bytes, StandardCharsets.UTF_8);
        if (s.indexOf('\uFFFD') >= 0) {
            s = new String(bytes, Charset.forName("GBK"));
        }
        return s.startsWith(CsvIo.BOM) ? s.substring(1) : s;
    }

    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return StrUtil.blankToDefault(t.getMessage(), e.getClass().getSimpleName());
    }
}
