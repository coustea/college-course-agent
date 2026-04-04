package com.ccut.service;

/**
 * 文档生成服务接口
 *
 * <p>使用 Apache POI 生成 .docx 和 .xlsx 物理文件，
 * 并返回可访问的下载 URL 字符串。</p>
 */
public interface DocumentGeneratorService {

    /**
     * 生成 Excel (.xlsx) 文件
     *
     * @param title            文件标题（同时作为工作表名称和文件名前缀）
     * @param dataDescription  表格数据的结构化描述（表头 + 数据行）
     * @return 下载 URL 字符串，如 "/uploads/generated/2026-03-30/xxx-成绩单.xlsx"
     */
    String generateExcel(String title, String dataDescription);

    /**
     * 生成 Word (.docx) 文件
     *
     * @param title              文档标题
     * @param contentDescription 文档完整内容描述（含章节标记）
     * @return 下载 URL 字符串，如 "/uploads/generated/2026-03-30/xxx-教案.docx"
     */
    String generateWord(String title, String contentDescription);
}
