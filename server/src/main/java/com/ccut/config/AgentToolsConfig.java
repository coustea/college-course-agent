package com.ccut.config;

import com.ccut.service.WebSearchService;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;


@Configuration
public class AgentToolsConfig {

    // ======================== 联网搜索 ========================


    @Bean
    @Description("在互联网上搜索信息。当用户询问实时信息、最新新闻、政策时事、" +
            "或者你不确定的事实性信息时使用此工具。默认搜索中文信息。")
    public Function<WebSearchRequest, String> webSearch(WebSearchService webSearchService) {
        return request -> webSearchService.search(request.query());
    }

    public record WebSearchRequest(
            @ToolParam(description = "搜索关键词，简洁精准，优先使用中文")
            String query
    ) {}
}
