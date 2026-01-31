package com.ccut.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class SearchTool implements BiFunction<SearchRequest, ToolContext, String> {

    @Override
    public String apply(SearchRequest request, ToolContext toolContext) {
        String query = request.getQuery();
        try {
            Map<String, Object> parameter = new HashMap<>();

            // 配置SerpAPI搜索参数
            parameter.put("engine", "google"); // 使用Google搜索引擎
            parameter.put("q", query); // 搜索关键词
            parameter.put("hl", "zh-cn"); // 语言：中文
            parameter.put("gl", "cn"); // 地区：中国
            parameter.put("api_key", "e0140cf1abc178499670081d6faff25bb5f75583fc10416c75af231efe9c7b44"); // SerpAPI密钥

            String url = "https://serpapi.com/search";

            String result = HttpUtil.get(url, parameter, 10000); // 发送HTTP GET请求

            // 解析JSON结果
            JSONObject jsonObject = JSONUtil.parseObj(result);

            // 优先级1: 检查answer_box_list（答案列表，如计算结果、天气等）
            if (jsonObject.containsKey("answer_box_list")) {
                JSONArray answerBoxList = jsonObject.getJSONArray("answer_box_list");
                if (answerBoxList != null && !answerBoxList.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < answerBoxList.size(); i++) {
                        sb.append(answerBoxList.getStr(i)).append("\n");
                    }
                    return sb.toString();
                }
            }

            // 优先级2: 检查answer_box中的answer（直接答案）
            if (jsonObject.containsKey("answer_box")) {
                JSONObject answerBox = jsonObject.getJSONObject("answer_box");
                if (answerBox != null && answerBox.containsKey("answer")) {
                    return answerBox.getStr("answer");
                }
            }

            // 优先级3: 检查knowledge_graph中的description（知识图谱描述）
            if (jsonObject.containsKey("knowledge_graph")) {
                JSONObject knowledgeGraph = jsonObject.getJSONObject("knowledge_graph");
                if (knowledgeGraph != null && knowledgeGraph.containsKey("description")) {
                    return knowledgeGraph.getStr("description");
                }
            }

            // 优先级4: 返回organic_results前3个搜索结果摘要
            if (jsonObject.containsKey("organic_results")) {
                JSONArray organicResults = jsonObject.getJSONArray("organic_results");
                if (organicResults != null && !organicResults.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int limit = Math.min(3, organicResults.size()); // 最多返回3条
                    for (int i = 0; i < limit; i++) {
                        JSONObject res = organicResults.getJSONObject(i);
                        String title = res.getStr("title", "");
                        String snippet = res.getStr("snippet", "");
                        sb.append("[").append(i + 1).append("] ").append(title).append("\n")
                          .append(snippet).append("\n\n");
                    }
                    return sb.toString();
                }
            }

            // 未找到搜索结果
            return "对不起，没有找到关于 '" + query + "' 的信息。";

        } catch (Exception e) {
            return "搜索时发生错误: " + e.getMessage();
        }
    }
}
