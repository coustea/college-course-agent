package com.ccut.service;

/**
 * 联网搜索服务接口
 *
 * <p>实现类应遵循以下日志规范：</p>
 * <ul>
 *     <li>在方法入口处添加 DEBUG 级别日志，记录搜索关键词</li>
 *     <li>在调用外部 API 前后添加 INFO 级别日志，记录搜索操作和耗时</li>
 *     <li>在异常处理处添加 ERROR 级别日志，记录错误信息</li>
 *     <li>在方法返回处添加 DEBUG 级别日志，记录结果摘要</li>
 * </ul>
 *
 * <p>日志格式示例：</p>
 * <pre>
 * log.debug("收到搜索请求：query={}", query);
 * log.info("执行联网搜索：query={}, 耗时={} ms", query, costTime);
 * log.error("联网搜索失败：query={}, 错误：{}", query, e.getMessage(), e);
 * log.debug("搜索完成：query={}, 结果长度={}", query, result.length());
 * </pre>
 */
public interface WebSearchService {

    /**
     * 执行搜索并返回摘要文本
     * 
     * @param query 搜索关键词
     * @return 搜索结果摘要
     * 
     * 日志记录点：
     * 1. 入口：log.debug("收到搜索请求：query={}", query);
     * 2. 业务：log.info("执行联网搜索：query={}", query);
     * 3. 耗时：log.info("联网搜索完成：query={}, 耗时={} ms", query, costTime);
     * 4. 返回：log.debug("搜索返回：query={}, 结果长度={}", query, result.length());
     * 5. 异常：log.error("联网搜索失败：query={}, 错误：{}", query, e.getMessage(), e);
     */
    String search(String query);
}
