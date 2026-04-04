package com.ccut.service.Impl;

import com.ccut.service.DocumentGeneratorService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档生成服务实现 — 基于 Apache POI
 *
 * <p>将 LLM 生成的结构化描述文本解析为 POI 操作，
 * 生成 .xlsx / .docx 文件并保存到磁盘，返回下载 URL。</p>
 */
@Service
public class DocumentGeneratorServiceImpl implements DocumentGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DocumentGeneratorServiceImpl.class);

    private static final int MAX_EXCEL_ROWS = 1000;
    private static final int MAX_WORD_CHARS = 50000;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ======================== Excel 生成 ========================

    @Override
    public String generateExcel(String title, String dataDescription) {
        log.info("开始生成 Excel 文件: title={}", title);
        long startTime = System.currentTimeMillis();

        try {
            // 1. 解析描述文本为结构化数据
            ParsedTableData tableData = parseTableData(dataDescription);

            if (tableData.headers().isEmpty()) {
                return "生成失败：未能从描述中解析出表头信息。请确保 dataDescription 包含表头，格式如：表头: 姓名,成绩,排名";
            }

            // 2. 创建工作簿
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                String sheetName = sanitizeSheetName(title);
                XSSFSheet sheet = workbook.createSheet(sheetName);

                // 3. 创建表头样式
                XSSFCellStyle headerStyle = createHeaderStyle(workbook);

                // 4. 写入表头行
                XSSFRow headerRow = sheet.createRow(0);
                List<String> headers = tableData.headers();
                for (int i = 0; i < headers.size(); i++) {
                    XSSFCell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }

                // 5. 写入数据行
                List<List<String>> rows = tableData.rows();
                int rowLimit = Math.min(rows.size(), MAX_EXCEL_ROWS);
                for (int i = 0; i < rowLimit; i++) {
                    XSSFRow row = sheet.createRow(i + 1);
                    List<String> rowData = rows.get(i);
                    for (int j = 0; j < headers.size(); j++) {
                        XSSFCell cell = row.createCell(j);
                        cell.setCellValue(j < rowData.size() ? rowData.get(j) : "");
                    }
                }

                // 6. 自动调整列宽
                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                    // 设置最小和最大列宽
                    int colWidth = sheet.getColumnWidth(i);
                    if (colWidth < 2000) colWidth = 2000;
                    if (colWidth > 15000) colWidth = 15000;
                    sheet.setColumnWidth(i, colWidth);
                }

                // 7. 写入文件
                byte[] content = workbookToBytes(workbook);
                String url = saveFile(content, "xlsx", title);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Excel 生成成功: title={}, headers={}, rows={}, 耗时={}ms, url={}",
                        title, headers.size(), rowLimit, duration, url);

                return "Excel 文件已生成！\n- 标题: " + title + "\n- 工作表: " + sheetName +
                        "\n- 表头: " + headers.size() + " 列\n- 数据: " + rowLimit + " 行" +
                        "\n下载链接: [" + title + ".xlsx](" + url + ")";
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Excel 生成失败: title={}, 耗时={}ms, error={}", title, duration, e.getMessage(), e);
            return "Excel 文件生成失败: " + e.getMessage();
        }
    }

    // ======================== Word 生成 ========================

    @Override
    public String generateWord(String title, String contentDescription) {
        log.info("开始生成 Word 文件: title={}", title);
        long startTime = System.currentTimeMillis();

        try {
            // 截断过长内容
            String content = contentDescription;
            if (content.length() > MAX_WORD_CHARS) {
                content = content.substring(0, MAX_WORD_CHARS) + "\n\n(内容过长，已截断)";
                log.warn("Word 文档内容超过 {} 字符，已截断", MAX_WORD_CHARS);
            }

            // 解析为章节结构
            List<DocumentSection> sections = parseDocumentSections(content);

            try (XWPFDocument document = new XWPFDocument()) {

                // 1. 写入标题
                XWPFParagraph titlePara = document.createParagraph();
                titlePara.setAlignment(ParagraphAlignment.CENTER);
                titlePara.setSpacingAfter(400);
                XWPFRun titleRun = titlePara.createRun();
                titleRun.setText(title);
                titleRun.setBold(true);
                titleRun.setFontSize(22);
                titleRun.setFontFamily("微软雅黑");

                // 2. 写入分隔线
                XWPFParagraph separator = document.createParagraph();
                separator.setBorderBottom(Borders.SINGLE);
                separator.setSpacingAfter(200);

                // 3. 写入各章节
                for (DocumentSection section : sections) {
                    if (section.isHeading()) {
                        // 章节标题
                        XWPFParagraph headingPara = document.createParagraph();
                        headingPara.setSpacingBefore(300);
                        headingPara.setSpacingAfter(100);
                        XWPFRun headingRun = headingPara.createRun();
                        headingRun.setText(section.text());
                        headingRun.setBold(true);
                        headingRun.setFontSize(16);
                        headingRun.setFontFamily("微软雅黑");
                    } else {
                        // 正文段落（按换行符分段）
                        String[] lines = section.text().split("\n");
                        for (String line : lines) {
                            String trimmed = line.trim();
                            if (trimmed.isEmpty()) continue;

                            XWPFParagraph bodyPara = document.createParagraph();
                            bodyPara.setSpacingAfter(80);
                            XWPFRun bodyRun = bodyPara.createRun();
                            bodyRun.setText(trimmed);
                            bodyRun.setFontSize(11);
                            bodyRun.setFontFamily("宋体");
                        }
                    }
                }

                // 4. 写入文件
                byte[] docContent = documentToBytes(document);
                String url = saveFile(docContent, "docx", title);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Word 生成成功: title={}, sections={}, 耗时={}ms, url={}",
                        title, sections.size(), duration, url);

                return "Word 文档已生成！\n- 标题: " + title +
                        "\n- 章节数: " + sections.size() +
                        "\n下载链接: [" + title + ".docx](" + url + ")";
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Word 生成失败: title={}, 耗时={}ms, error={}", title, duration, e.getMessage(), e);
            return "Word 文档生成失败: " + e.getMessage();
        }
    }

    // ======================== Excel 解析 ========================

    /**
     * 将 LLM 输出的表格描述解析为结构化数据
     *
     * 支持格式：
     * - "表头: 姓名,成绩,排名; 数据行: 张三,95,1; 李四,88,2"
     * - 每行一条记录，逗号或制表符分隔
     * - 第一行为表头
     */
    private ParsedTableData parseTableData(String dataDescription) {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        if (dataDescription == null || dataDescription.isBlank()) {
            return new ParsedTableData(headers, rows);
        }

        // 尝试格式1: "表头: xxx; 数据行: xxx"
        String headerPart = extractSection(dataDescription, "表头");
        if (headerPart != null) {
            headers.addAll(parseLine(headerPart));

            String dataPart = extractSection(dataDescription, "数据行");
            if (dataPart != null) {
                // 数据行可能用分号分隔
                String[] dataLines = dataPart.split("[;；]");
                for (String line : dataLines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        rows.add(parseLine(trimmed));
                    }
                }
            }
            return new ParsedTableData(headers, rows);
        }

        // 格式2: 纯文本行，第一行为表头
        String[] lines = dataDescription.split("\n");
        boolean firstLine = true;
        for (String line : lines) {
            String trimmed = line.trim();
            // 跳过空行和纯标记行
            if (trimmed.isEmpty() || trimmed.matches("^[#\\-*=]+$")) continue;

            List<String> cells = parseLine(trimmed);
            if (cells.isEmpty()) continue;

            if (firstLine) {
                headers.addAll(cells);
                firstLine = false;
            } else {
                rows.add(cells);
            }
        }

        return new ParsedTableData(headers, rows);
    }

    /**
     * 从描述文本中提取指定标签的内容
     * 例如 extractSection("表头: 姓名,年龄; 数据行: ...", "表头") → "姓名,年龄"
     */
    private String extractSection(String text, String tag) {
        // 匹配 "表头:" 或 "表头：" 或 "headers:"
        Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(tag) + "\\s*[:：]\\s*(.+?)(?=;|数据|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 解析单行数据，支持逗号、制表符、竖线分隔
     */
    private List<String> parseLine(String line) {
        List<String> cells = new ArrayList<>();
        // 去除行首序号（如 "1. " 或 "1、"）
        String cleaned = line.replaceFirst("^\\d+[.、)）]\\s*", "");
        // 按分隔符切分
        String[] parts = cleaned.split("[,，\t|]");
        for (String part : parts) {
            String cell = part.trim();
            if (!cell.isEmpty()) {
                cells.add(cell);
            }
        }
        return cells;
    }

    /**
     * 创建 Excel 表头单元格样式（浅蓝背景 + 加粗白色字体）
     */
    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        // 背景色：浅蓝
        XSSFColor bgColor = new XSSFColor(new byte[]{(byte) 68, (byte) 114, (byte) 196}, null);
        style.setFillForegroundColor(bgColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // 字体：加粗白色
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 255}, null));
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        // 居中
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        // 边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 工作表名称清理（Excel 要求 ≤ 31 字符且不含特殊字符）
     */
    private String sanitizeSheetName(String name) {
        if (name == null || name.isBlank()) return "Sheet1";
        String sanitized = name.replaceAll("[\\\\/:*?\"<>\\[\\]]", "_");
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized;
    }

    // ======================== Word 解析 ========================

    /**
     * 将文档内容解析为章节列表
     *
     * 支持的标题格式：
     * - "一、标题" / "二、标题" / "三、标题" ...
     * - "1. 标题" / "2. 标题" ...
     * - "## 标题" / "### 标题" ...
     * - "第X章 标题" / "第X节 标题"
     */
    private List<DocumentSection> parseDocumentSections(String content) {
        List<DocumentSection> sections = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return sections;
        }

        // 章节标题正则
        Pattern headingPattern = Pattern.compile(
                "^(?:#{1,4}\\s+|\\d+[.、)）]\\s*|第[一二三四五六七八九十百]+[章节]\\s+)(.+)$",
                Pattern.MULTILINE
        );

        // 中文数字标题（一、二、三、）
        Pattern chineseNumPattern = Pattern.compile(
                "^[一二三四五六七八九十]+、\\s*(.+)$",
                Pattern.MULTILINE
        );

        // 按行扫描，识别标题和正文
        String[] lines = content.split("\n");
        StringBuilder bodyBuffer = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                bodyBuffer.append("\n");
                continue;
            }

            boolean isHeading = false;

            // 检查 Markdown 标题
            Matcher mdMatcher = headingPattern.matcher(trimmed);
            if (mdMatcher.matches()) {
                isHeading = true;
                trimmed = mdMatcher.group(1).trim();
            }

            // 检查中文数字标题
            if (!isHeading) {
                Matcher cnMatcher = chineseNumPattern.matcher(trimmed);
                if (cnMatcher.matches()) {
                    isHeading = true;
                    trimmed = cnMatcher.group(1).trim();
                }
            }

            if (isHeading) {
                // 先刷新之前的正文缓冲
                flushBodyBuffer(sections, bodyBuffer);
                sections.add(new DocumentSection(true, trimmed));
            } else {
                bodyBuffer.append(line).append("\n");
            }
        }

        // 刷新最后的正文
        flushBodyBuffer(sections, bodyBuffer);

        return sections;
    }

    /**
     * 将缓冲的正文文本作为一个正文 section 添加到列表
     */
    private void flushBodyBuffer(List<DocumentSection> sections, StringBuilder buffer) {
        if (!buffer.isEmpty()) {
            String body = buffer.toString().trim();
            if (!body.isEmpty()) {
                sections.add(new DocumentSection(false, body));
            }
            buffer.setLength(0);
        }
    }

    // ======================== 通用工具方法 ========================

    /**
     * 将 XSSFWorkbook 写入字节数组
     */
    private byte[] workbookToBytes(XSSFWorkbook workbook) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 将 XWPFDocument 写入字节数组
     */
    private byte[] documentToBytes(XWPFDocument document) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            document.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 将生成的文件保存到磁盘，返回可访问的 URL 路径
     *
     * 保存路径: {uploadDir}/generated/{yyyy-MM-dd}/{uuid}-{title}.{ext}
     * URL 路径: /uploads/generated/{yyyy-MM-dd}/{uuid}-{title}.{ext}
     */
    private String saveFile(byte[] content, String extension, String title) throws IOException {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path dirPath = Paths.get(uploadDir, "generated", dateDir);
        Files.createDirectories(dirPath);

        // 清理文件名中的特殊字符
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeTitle.length() > 50) {
            safeTitle = safeTitle.substring(0, 50);
        }

        String filename = UUID.randomUUID().toString().substring(0, 8) + "-" + safeTitle + "." + extension;
        Path filePath = dirPath.resolve(filename);
        Files.write(filePath, content);

        return "/uploads/generated/" + dateDir + "/" + filename;
    }

    // ======================== 内部数据类 ========================

    /**
     * 解析后的表格数据（表头 + 数据行）
     */
    private record ParsedTableData(List<String> headers, List<List<String>> rows) {}

    /**
     * 文档章节（标题或正文）
     */
    private record DocumentSection(boolean isHeading, String text) {}
}
