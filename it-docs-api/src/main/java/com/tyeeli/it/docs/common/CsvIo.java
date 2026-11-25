package com.tyeeli.it.docs.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 极简 RFC4180 CSV 读写（不引 POI/EasyExcel，Maven 离线也能构建）。
 * 写入时所有字段统一加引号并转义内部引号，正文里的逗号、换行、双引号都不会破坏表格；
 * 写出时文件头带 UTF-8 BOM，Excel 双击直接打开中文不乱码。
 */
public final class CsvIo {

    /**
     * 导入/导出统一列（顺序可任意，导入按表头名匹配）
     */
    public static final String[] HEADERS = {
            "docNo", "title", "docType", "spaceId", "system", "category",
            "owner", "summary", "tags", "contentMd", "contentJson"
    };

    /**
     * 导出/模板的中文表头（与 HEADERS 一一对应；导入端 headerIndex 认这些中文名，可原样导回）
     */
    public static final String[] CN_HEADERS = {
            "编号", "标题", "类型", "空间ID", "归属系统", "问题分类",
            "处理角色", "摘要", "标签", "正文", "问答结构化"
    };

    /** UTF-8 BOM，Excel 识别编码用 */
    public static final String BOM = "\uFEFF";

    private CsvIo() {
    }

    /** 单元格转义：统一加引号，内部 " 变 "" */
    public static String cell(String s) {
        String v = s == null ? "" : s;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    /** 拼一行 CSV（不含换行符） */
    public static String row(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(cell(cells[i]));
        }
        return sb.toString();
    }

    /**
     * 解析 CSV 文本：支持引号内逗号/换行/双写引号转义。
     *
     * @param text 已去掉 BOM 的文本
     */
    public static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<String[]>();
        List<String> cur = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean touched = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }
            if (c == '"') {
                inQuotes = true;
                touched = true;
            } else if (c == ',') {
                cur.add(field.toString());
                field.setLength(0);
                touched = true;
            } else if (c == '\n') {
                cur.add(field.toString());
                field.setLength(0);
                rows.add(cur.toArray(new String[0]));
                cur.clear();
                touched = false;
            } else if (c != '\r') {
                field.append(c);
                touched = true;
            }
        }
        if (touched || field.length() > 0 || !cur.isEmpty()) {
            cur.add(field.toString());
            rows.add(cur.toArray(new String[0]));
        }
        return rows;
    }
}
