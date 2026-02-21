package com.ccut.service.Impl;

import com.ccut.dto.Attachment;
import com.ccut.service.DocumentAnalysisService;
import com.ccut.utils.ReadFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 文档分析服务实现 — 使用专用线程池异步解析文档
 */
@Service
public class DocumentAnalysisServiceImpl implements DocumentAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentAnalysisServiceImpl.class);

    @Override
    @Async("documentAnalysisExecutor")
    public CompletableFuture<String> analyzeAsync(Attachment attachment) {
        logger.info("异步解析文档: {}", attachment.filename());
        try {
            String content = ReadFileUtils.readFileContent(attachment.url(), attachment.filename());
            logger.info("文档解析完成: {} ({}字符)", attachment.filename(), content.length());
            return CompletableFuture.completedFuture(content);
        } catch (Exception e) {
            logger.error("文档解析失败: {}", attachment.filename(), e);
            return CompletableFuture.completedFuture("文档解析失败: " + attachment.filename());
        }
    }

    @Override
    public String analyzeAllAttachments(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return "";

        List<Attachment> documents = new ArrayList<>();
        for (Attachment att : attachments) {
            String ext = getExtension(att.filename());
            if (!ReadFileUtils.isImageFile(ext)) {
                documents.add(att);
            }
        }

        if (documents.isEmpty()) return "";

        // 并行解析所有文档
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (Attachment doc : documents) {
            futures.add(analyzeAsync(doc));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n[附件内容]\n");
        for (CompletableFuture<String> f : futures) {
            try {
                sb.append(f.get()).append("\n");
            } catch (Exception e) {
                sb.append("解析失败\n");
            }
        }
        return sb.toString();
    }

    @Override
    public List<Attachment> getImageAttachments(List<Attachment> attachments) {
        List<Attachment> images = new ArrayList<>();
        if (attachments == null) return images;
        for (Attachment att : attachments) {
            String ext = getExtension(att.filename());
            if (ReadFileUtils.isImageFile(ext)) {
                images.add(att);
            }
        }
        return images;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
