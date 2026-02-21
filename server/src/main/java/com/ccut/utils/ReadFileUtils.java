package com.ccut.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * 文件读取工具类
 * 用于读取各种文件格式的内容，以便AI理解
 */
public class ReadFileUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReadFileUtils.class);

    private static final java.util.Set<String> IMAGE_EXTENSIONS = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    );

    /**
     * 判断文件扩展名是否为图片
     */
    public static boolean isImageFile(String extension) {
        return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * 读取图片文件并返回纯 base64 字符串（供 Vision API 使用）
     */
    public static String readImageAsBase64(File file) throws IOException {
        byte[] imageBytes = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 获取图片的 MIME 类型
     */
    public static String getImageMimeType(String extension) {
        if (extension == null) return "image/jpeg";
        return switch (extension.toLowerCase()) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            default -> "image/jpeg";
        };
    }

    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @param filename 文件名
     * @return 文件内容描述（文本内容或base64编码）
     */
    public static String readFileContent(String filePath, String filename) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null) {
            return "不支持的文件类型";
        }

        extension = extension.toLowerCase();

        try {
            // 处理文件路径（去掉URL前缀）
            String actualPath = filePath;
            if (filePath.startsWith("/uploads/")) {
                // 从URL提取相对路径
                String relativePath = filePath.replace("/uploads/", "");
                // 假设文件存储在配置的upload.base-dir目录下
                // 这里需要根据实际情况调整路径
                actualPath = "uploads/" + relativePath;
            }

            Path path = Paths.get(actualPath);
            if (!Files.exists(path)) {
                logger.warn("File not found: {}", actualPath);
                return "文件不存在";
            }

            File file = path.toFile();

            // 根据文件类型读取内容
            switch (extension) {
                case "pdf":
                    return readPdfFile(file);

                case "doc":
                    return readDocFile(file);

                case "docx":
                    return readDocxFile(file);

                case "xls":
                case "xlsx":
                    return readExcelFile(file);

                case "ppt":
                    return readPptFile(file);

                case "pptx":
                    return readPptxFile(file);

                case "csv":
                    return readCsvFile(file);

                case "txt":
                case "md":
                case "markdown":
                case "json":
                case "xml":
                case "html":
                case "css":
                case "js":
                case "java":
                case "py":
                case "c":
                case "cpp":
                case "sql":
                case "yaml":
                case "yml":
                case "properties":
                case "log":
                    return readTxtFile(file);

                case "jpg":
                case "jpeg":
                case "png":
                case "gif":
                case "bmp":
                case "webp":
                    return readImageFile(file);

                default:
                    return "不支持的文件类型: " + extension;
            }

        } catch (Exception e) {
            logger.error("Failed to read file: {}, error: {}", filePath, e.getMessage(), e);
            return "文件读取失败: " + e.getMessage();
        }
    }

    /**
     * 读取PDF文件内容
     */
    private static String readPdfFile(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // 限制文本长度，避免token过多
            if (text.length() > 10000) {
                text = text.substring(0, 10000) + "\n...(内容过长，已截断)";
            }
            return "[PDF文档内容]\n" + text;
        }
    }

    /**
     * 读取旧版Word文档（.doc）
     */
    private static String readDocFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            String text = extractor.getText();
            if (text.length() > 10000) {
                text = text.substring(0, 10000) + "\n...(内容过长，已截断)";
            }
            return "[Word文档内容]\n" + text;
        }
    }

    /**
     * 读取新版Word文档（.docx）
     */
    private static String readDocxFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder text = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                text.append(para.getText()).append("\n");
            }
            String content = text.toString();
            if (content.length() > 10000) {
                content = content.substring(0, 10000) + "\n...(内容过长，已截断)";
            }
            return "[Word文档内容]\n" + content;
        }
    }

    /**
     * 读取Excel文件（.xls, .xlsx）
     */
    private static String readExcelFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            StringBuilder content = new StringBuilder();
            content.append("[Excel表格内容]\n");

            int sheetCount = workbook.getNumberOfSheets();
            for (int s = 0; s < Math.min(sheetCount, 3); s++) { // 最多读取3个sheet
                Sheet sheet = workbook.getSheetAt(s);
                content.append("\n工作表").append(s + 1).append(": ").append(sheet.getSheetName()).append("\n");

                int rowCount = sheet.getPhysicalNumberOfRows();
                for (int r = 0; r < Math.min(rowCount, 50); r++) { // 每个sheet最多读取50行
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        StringBuilder rowText = new StringBuilder();
                        int cellCount = row.getPhysicalNumberOfCells();
                        for (int c = 0; c < cellCount; c++) {
                            Cell cell = row.getCell(c);
                            String cellValue = getCellValue(cell);
                            rowText.append(cellValue).append("\t");
                        }
                        content.append(rowText).append("\n");
                    }
                }

                if (rowCount > 50) {
                    content.append("...(数据过多，仅显示前50行)\n");
                }
            }

            if (sheetCount > 3) {
                content.append("...(工作表过多，仅显示前3个)\n");
            }

            return content.toString();

        } catch (Exception e) {
            logger.error("Failed to read Excel file: {}", e.getMessage(), e);
            return "Excel文件读取失败: " + e.getMessage();
        }
    }

    /**
     * 获取单元格值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 读取纯文本文件
     */
    private static String readTxtFile(File file) throws IOException {
        String text = Files.readString(file.toPath());
        if (text.length() > 10000) {
            text = text.substring(0, 10000) + "\n...(内容过长，已截断)";
        }
        return "[文本文件内容]\n" + text;
    }

    /**
     * 读取图片文件（转换为base64编码）
     */
    private static String readImageFile(File file) throws IOException {
        byte[] imageBytes = Files.readAllBytes(file.toPath());
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        if (base64.length() > 100000) {
            return "[图片文件]\n文件名: " + file.getName() +
                   "\n大小: " + file.length() + " bytes" +
                   "\n图片过大，建议用户描述图片内容或分段上传";
        }

        return "[图片文件]\n文件名: " + file.getName() +
               "\n大小: " + file.length() + " bytes" +
               "\nBase64编码: " + base64;
    }

    /**
     * 读取旧版PPT文件（.ppt）
     */
    private static String readPptFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HSLFSlideShow ppt = new HSLFSlideShow(fis);
             SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(ppt)) {
            String text = extractor.getText();
            if (text.length() > 10000) {
                text = text.substring(0, 10000) + "\n...(内容过长，已截断)";
            }
            return "[PPT演示文稿内容]\n" + text;
        }
    }

    /**
     * 读取新版PPT文件（.pptx）
     */
    private static String readPptxFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow pptx = new XMLSlideShow(fis)) {
            StringBuilder text = new StringBuilder();
            text.append("[PPTX演示文稿内容]\n");
            List<XSLFSlide> slides = pptx.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                text.append("\n--- 第").append(i + 1).append("页 ---\n");
                XSLFSlide slide = slides.get(i);
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        text.append(textShape.getText()).append("\n");
                    }
                }
            }
            String content = text.toString();
            if (content.length() > 10000) {
                content = content.substring(0, 10000) + "\n...(内容过长，已截断)";
            }
            return content;
        }
    }

    /**
     * 读取CSV文件
     */
    private static String readCsvFile(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        StringBuilder content = new StringBuilder();
        content.append("[CSV文件内容]\n");
        int maxLines = Math.min(lines.size(), 100);
        for (int i = 0; i < maxLines; i++) {
            content.append(lines.get(i)).append("\n");
        }
        if (lines.size() > 100) {
            content.append("...(共").append(lines.size()).append("行，仅显示前100行)\n");
        }
        return content.toString();
    }
}