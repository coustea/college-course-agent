package com.ccut.service.Impl;

import com.ccut.service.WebSearchService;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 联网搜索服务实现（SerpAPI）
 */
@Service
public class WebSearchServiceImpl implements WebSearchService {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchServiceImpl.class);
    private static final String SERP_API_KEY = "e0140cf1abc178499670081d6faff25bb5f75583fc10416c75af231efe9c7b44";
    private static final String SERP_API_URL = "https://serpapi.com/search";

    @Override
    public String search(String query) {
        try {
            logger.info("WebSearch: query={}", query);

            Map<String, Object> params = new HashMap<>();
            params.put("engine", "google");
            params.put("q", query);
            params.put("hl", "zh-cn");
            params.put("gl", "cn");
            params.put("api_key", SERP_API_KEY);

            String result = HttpUtil.get(SERP_API_URL, params, 10000);
            JSONObject json = JSONUtil.parseObj(result);

            // 优先：answer_box
            if (json.containsKey("answer_box")) {
                JSONObject answerBox = json.getJSONObject("answer_box");
                if (answerBox != null && answerBox.containsKey("answer")) {
                    return answerBox.getStr("answer");
                }
                if (answerBox != null && answerBox.containsKey("snippet")) {
                    return answerBox.getStr("snippet");
                }
            }

            // 其次：knowledge_graph
            if (json.containsKey("knowledge_graph")) {
                JSONObject kg = json.getJSONObject("knowledge_graph");
                if (kg != null && kg.containsKey("description")) {
                    return kg.getStr("description");
                }
            }

            // 最后：organic_results 前3条
            if (json.containsKey("organic_results")) {
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
                    return sb.toString();
                }
            }

            return "未找到相关搜索结果";

        } catch (Exception e) {
            logger.error("WebSearch failed: {}", e.getMessage(), e);
            return "搜索失败: " + e.getMessage();
        }
    }
}
