package com.ccut.service;

import com.ccut.dto.Attachment;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 文档分析服务接口
 */
public interface DocumentAnalysisService {

    /**
     * 异步分析单个附件
     */
    CompletableFuture<String> analyzeAsync(Attachment attachment);

    /**
     * 并行解析多个附件，返回合并后的文本
     */
    String analyzeAllAttachments(List<Attachment> attachments);

    /**
     * 获取图片附件列表
     */
    List<Attachment> getImageAttachments(List<Attachment> attachments);
}
