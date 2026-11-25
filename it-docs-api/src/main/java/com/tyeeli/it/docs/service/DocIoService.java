package com.tyeeli.it.docs.service;

import com.tyeeli.it.docs.dto.DocImportResultDTO;
import com.tyeeli.it.docs.dto.DocQuery;

/**
 * 文档批量导入导出（CSV，Excel 可直接打开）。
 * 列定义见 CsvIo.HEADERS；导入按表头名匹配，顺序可任意。
 */
public interface DocIoService {

    /**
     * 按列表查询条件导出全量匹配文档为 CSV 文本（带 BOM），最多 5000 条。
     * 中文表头（CsvIo.CN_HEADERS），导出文件可原样再导入。
     */
    String exportCsv(DocQuery query, String currentUser, String globalRole, boolean ldapUser);

    /**
     * 导入模板：表头 + 一行示例。
     */
    String templateCsv();

    /**
     * 解析 CSV 并逐行建档/更新；单行失败不中断后续行。
     *
     * @param defaultSpaceId  行里没写 spaceId 时使用的目标空间
     * @param updateExisting  已存在（按编号或同空间同标题）时是否更新，false 则跳过
     */
    DocImportResultDTO importCsv(byte[] bytes, Long defaultSpaceId, boolean updateExisting,
                                String operator, String globalRole, boolean ldapUser);
}
