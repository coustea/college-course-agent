package com.ccut.service.Impl;

import com.ccut.service.WebSearchService;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 联网搜索服务实现（SerpAPI）
 */
@Service
public class WebSearchServiceImpl implements WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchServiceImpl.class);
    private static final String SERP_API_URL = "https://serpapi.com/search";

    @Value("${serp.api-key:}")
    private String serpApiKey;

    @Override
    public String search(String query) {
        log.debug("执行方法：search, 参数：query={}", query);
        long startTime = System.currentTimeMillis();
        try {
            log.info("开始联网搜索：query={}", query);

            Map<String, Object> params = new HashMap<>();
            params.put("engine", "google");
            params.put("q", query);
            params.put("hl", "zh-cn");
            params.put("gl", "cn");
            params.put("api_key", serpApiKey);

            log.debug("发送请求到 SerpAPI: url={}, params={}", SERP_API_URL, params.keySet());
            String result = HttpUtil.get(SERP_API_URL, params, 10000);
            JSONObject json = JSONUtil.parseObj(result);

            String searchResult = null;

            // 优先：answer_box
            if (json.containsKey("answer_box")) {
                JSONObject answerBox = json.getJSONObject("answer_box");
                if (answerBox != null && answerBox.containsKey("answer")) {
                    searchResult = answerBox.getStr("answer");
                    log.info("从 answer_box 获取结果：{}", truncate(searchResult, 50));
                } else if (answerBox != null && answerBox.containsKey("snippet")) {
                    searchResult = answerBox.getStr("snippet");
                    log.info("从 answer_box.snippet 获取结果：{}", truncate(searchResult, 50));
                }
            }

            // 其次：knowledge_graph
            if (searchResult == null && json.containsKey("knowledge_graph")) {
                JSONObject kg = json.getJSONObject("knowledge_graph");
                if (kg != null && kg.containsKey("description")) {
                    searchResult = kg.getStr("description");
                    log.info("从 knowledge_graph 获取结果：{}", truncate(searchResult, 50));
                }
            }

            // 最后：organic_results 前 3 条
            if (searchResult == null && json.containsKey("organic_results")) {
                JSONArray results = json.getJSONArray("organic_results");
                if (results != null && !results.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int limit = Math.min(3, results.size());
                    for (int i = 0; i < limit; i++) {
                        JSONObject r = results.getJSONObject(i);
                        sb.append("[").append(i + 1).append("] ")
                          .append(r.getStr("title", "")).append("\n")
                          .append(r.getStr("snippet", "")).append("\n\n");
                    }
                    searchResult = sb.toString();
                    log.info("从 organic_results 获取结果：共{}条", limit);
                }
            }

            if (searchResult == null) {
                searchResult = "未找到相关搜索结果";
                log.warn("未找到搜索结果：query={}", query);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("联网搜索完成：query={}, 耗时={}ms, 结果长度={}", query, duration, searchResult.length());
            log.debug("方法返回：result length={}", searchResult.length());
            return searchResult;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("联网搜索失败：query={}, 耗时={}ms, error={}", query, duration, e.getMessage(), e);
            return "搜索失败：" + e.getMessage();
        }
    }

    /**
     * 截断字符串用于日志显示
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
