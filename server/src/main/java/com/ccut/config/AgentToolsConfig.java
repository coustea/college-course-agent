package com.ccut.config;

import com.ccut.service.DocumentGeneratorService;
import com.ccut.service.WebSearchService;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Agent 工具注册配置类
 *
 * <p>将三大 Agent 能力注册为 Spring AI 标准的 Function Tool，
 * 供 ChatClient 通过 .defaultTools(...) 挂载给大模型自动调用：</p>
 * <ul>
 *     <li>webSearch — 联网搜索</li>
 *     <li>generateExcel — 生成 Excel 文件</li>
 *     <li>generateWord — 生成 Word 文件</li>
 * </ul>
 */
@Configuration
public class AgentToolsConfig {

    // ======================== Tool 1: 联网搜索 ========================

    /**
     * 联网搜索工具 — 委托给现有的 WebSearchService（SerpAPI）
     */
    @Bean
    @Description("在互联网上搜索信息。当用户询问实时信息、最新新闻、政策时事、" +
            "或者你不确定的事实性信息时使用此工具。默认搜索中文信息。")
    public Function<WebSearchRequest, String> webSearch(WebSearchService webSearchService) {
        return request -> webSearchService.search(request.query());
    }

    // ======================== Tool 2: 生成 Excel ========================

    /**
     * Excel 文件生成工具 — 委托给 DocumentGeneratorService
     */
    @Bean
    @Description("生成 Excel (.xlsx) 文件并返回下载链接。当用户要求生成表格、成绩单、" +
            "数据报表、统计表、课程表等 Excel 格式内容时使用此工具。" +
            "dataDescription 的推荐格式：表头: 列1,列2,列3; 数据行: 值1,值2,值3")
    public Function<GenerateExcelRequest, String> generateExcel(DocumentGeneratorService docGenService) {
        return request -> docGenService.generateExcel(request.title(), request.dataDescription());
    }

    // ======================== Tool 3: 生成 Word ========================

    /**
     * Word 文件生成工具 — 委托给 DocumentGeneratorService
     */
    @Bean
    @Description("生成 Word (.docx) 文件并返回下载链接。当用户要求生成教案、试卷、" +
            "教学方案、实验报告、课程大纲等 Word 格式文档时使用此工具。" +
            "contentDescription 应包含完整的文档内容，可使用章节标记如 一、二、三、 或 ## 标题 进行分段。")
    public Function<GenerateWordRequest, String> generateWord(DocumentGeneratorService docGenService) {
        return request -> docGenService.generateWord(request.title(), request.contentDescription());
    }

    // ======================== Tool 请求类型定义 ========================

    /**
     * 联网搜索请求
     */
    public record WebSearchRequest(
            @ToolParam(description = "搜索关键词，简洁精准，优先使用中文")
            String query
    ) {}

    /**
     * Excel 生成请求
     */
    public record GenerateExcelRequest(
            @ToolParam(description = "Excel 文件标题，同时作为工作表名称和文件名前缀")
            String title,
            @ToolParam(description = "Excel 表格数据的结构化描述。推荐格式：" +
                    "\"表头: 姓名,成绩,排名; 数据行: 张三,95,1; 李四,88,2\"")
            String dataDescription
    ) {}

    /**
     * Word 生成请求
     */
    public record GenerateWordRequest(
            @ToolParam(description = "Word 文档标题")
            String title,
            @ToolParam(description = "Word 文档的完整内容描述，包括章节标题和正文。" +
                    "可使用章节标记（如 一、标题 / ## 标题 / 1. 标题）进行分段。")
            String contentDescription
    ) {}
}
