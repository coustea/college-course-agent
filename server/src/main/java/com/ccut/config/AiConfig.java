package com.ccut.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * Spring AI 配置类
 * 手动构建 OpenAiChatModel（通过 OpenAI 兼容接口连接 DeepSeek）
 * 配置自定义 SSL 以解决 SSL 握手问题
 */
@Configuration
public class AiConfig {

    private static final Logger logger = LoggerFactory.getLogger(AiConfig.class);

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    static {
        try {
            // 配置信任所有证书的 TrustManager（仅用于开发环境）
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            }, null);

            // 设置默认的 SSLContext
            SSLContext.setDefault(sslContext);

            // 配置 HttpsURLConnection 使用宽松的主机名验证器
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            logger.info("SSL 配置已更新，允许所有证书（开发环境）");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("初始化 SSL 配置失败: {}", e.getMessage(), e);
        }
    }

    @Bean("chatModel")
    public ChatModel chatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        logger.info("AI 模型初始化成功（deepseek-chat）");

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model("deepseek-chat")
                                .temperature(temperature)
                                .build()
                )
                .build();
    }

    @Bean("reasoningChatModel")
    public ChatModel reasoningChatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        logger.info("AI 推理模型初始化成功（deepseek-reasoner）");

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model("deepseek-reasoner")
                                .temperature(temperature)
                                .build()
                )
                .build();
    }
}
