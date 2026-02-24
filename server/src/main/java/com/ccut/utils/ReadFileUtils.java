package com.ccut.utils;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
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
import java.util.Set;

/**
 * 文件读取工具类（使用 Apache Tika）
 * Apache Tika 是一个统一的内容检测和文本提取框架
 * 支持数百种文件格式：PDF、Word、Excel、PPT、图片、音频、视频等
 */
public class ReadFileUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReadFileUtils.class);

    private static final Tika tika = new Tika();
    private static final int MAX_TEXT_LENGTH = 10000;  // 最大文本长度
    private static final int MAX_IMAGE_SIZE = 100000;   // 最大图片大小（Base64编码前）

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
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
     * 读取文件内容（使用 Apache Tika）
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
                String relativePath = filePath.replace("/uploads/", "");
                actualPath = "uploads/" + relativePath;
            }

            Path path = Paths.get(actualPath);
            if (!Files.exists(path)) {
                logger.warn("File not found: {}", actualPath);
                return "文件不存在";
            }

            File file = path.toFile();

            // 图片文件特殊处理（返回 Base64）
            if (isImageFile(extension)) {
                return readImageFile(file);
            }

            // 使用 Apache Tika 统一处理所有文档类型
            return readDocumentWithTika(file, filename);

        } catch (Exception e) {
            logger.error("Failed to read file: {}, error: {}", filePath, e.getMessage(), e);
            return "文件读取失败: " + e.getMessage();
        }
    }

    /**
     * 使用 Apache Tika 读取文档内容
     * 支持：PDF、Word、Excel、PPT、TXT、CSV、HTML、XML、Markdown 等
     */
    private static String readDocumentWithTika(File file, String filename) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            // 使用 Tika 自动检测文件类型并提取文本
            String mimeType = tika.detect(file);
            logger.debug("检测到文件类型: {} - {}", filename, mimeType);

            // 提取文本内容
            String text = tika.parseToString(inputStream);

            // 检查文本是否为空
            if (text == null || text.trim().isEmpty()) {
                logger.warn("文件内容为空: {} (MIME类型: {})", filename, mimeType);

                // 根据文件类型给出提示
                if (mimeType != null && mimeType.contains("pdf")) {
                    return "[PDF文档内容]\n⚠️ 警告：此PDF文件无法提取文本内容。\n可能原因：\n1. 这是扫描版PDF（图片格式），没有文本层\n2. PDF文件已加密\n3. PDF文件损坏\n\n建议：\n- 如果是扫描版PDF，请使用OCR工具转换为可搜索的PDF\n- 或者手动复制PDF中的文本内容进行提问";
                } else if (mimeType != null && (mimeType.contains("word") || mimeType.contains("msword"))) {
                    return "[Word文档内容]\n⚠️ 警告：无法提取此Word文档的文本内容。\n可能原因：\n1. 文档已加密\n2. 文档格式不兼容\n3. 文档损坏";
                } else if (mimeType != null && mimeType.contains("sheet")) {
                    return "[Excel表格内容]\n⚠️ 警告：无法提取此Excel表格的文本内容。\n可能原因：\n1. 表格已加密\n2. 表格格式不兼容\n3. 表格损坏";
                } else if (mimeType != null && mimeType.contains("presentation")) {
                    return "[PPT演示文稿内容]\n⚠️ 警告：无法提取此PPT的文本内容。\n可能原因：\n1. 演示文稿已加密\n2. 格式不兼容\n3. 文件损坏";
                } else {
                    return "[文档内容]\n⚠️ 警告：无法提取此文件的文本内容。\n文件类型: " + mimeType;
                }
            }

            // 限制文本长度，避免token过多
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH) + "\n...(内容过长，已截断至" + MAX_TEXT_LENGTH + "字符)";
            }

            logger.info("成功读取文件: {}, MIME类型: {}, 文本长度: {}", filename, mimeType, text.length());

            // 根据MIME类型添加标签
            String tag = getDocumentTag(mimeType);
            return tag + "\n" + text;

        } catch (IOException e) {
            logger.error("读取文件失败: {}, error: {}", filename, e.getMessage(), e);
            return "文件读取失败: " + e.getMessage();
        } catch (TikaException e) {
            logger.error("Tika解析文件失败: {}, error: {}", filename, e.getMessage(), e);
            return "文档解析失败: " + e.getMessage();
        }
    }

    /**
     * 根据MIME类型返回文档标签
     */
    private static String getDocumentTag(String mimeType) {
        if (mimeType == null) {
            return "[文档内容]";
        }

        if (mimeType.contains("pdf")) {
            return "[PDF文档内容]";
        } else if (mimeType.contains("word") || mimeType.contains("msword")) {
            return "[Word文档内容]";
        } else if (mimeType.contains("sheet") || mimeType.contains("excel") || mimeType.contains("spreadsheet")) {
            return "[Excel表格内容]";
        } else if (mimeType.contains("powerpoint") || mimeType.contains("presentation")) {
            return "[PPT演示文稿内容]";
        } else if (mimeType.contains("text/plain")) {
            return "[文本文件内容]";
        } else if (mimeType.contains("csv")) {
            return "[CSV文件内容]";
        } else if (mimeType.contains("html")) {
            return "[HTML文件内容]";
        } else if (mimeType.contains("xml")) {
            return "[XML文件内容]";
        } else if (mimeType.contains("markdown") || mimeType.contains("md")) {
            return "[Markdown文件内容]";
        } else {
            return "[文档内容]";
        }
    }

    /**
     * 读取图片文件（转换为base64编码）
     */
    private static String readImageFile(File file) throws IOException {
        byte[] imageBytes = Files.readAllBytes(file.toPath());
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        if (base64.length() > MAX_IMAGE_SIZE) {
            return "[图片文件]\n文件名: " + file.getName() +
                   "\n大小: " + file.length() + " bytes" +
                   "\n图片过大，建议用户描述图片内容或分段上传";
        }

        return "[图片文件]\n文件名: " + file.getName() +
               "\n大小: " + file.length() + " bytes" +
               "\nBase64编码: " + base64;
    }
}
