package com.ccut.service;

/**
 * 联网搜索服务接口
 */
public interface WebSearchService {

    /**
     * 执行搜索并返回摘要文本
     * @param query 搜索关键词
     * @return 搜索结果摘要
     */
    String search(String query);
}
