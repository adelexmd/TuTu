package com.tutu.configuration;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 模型 Bean。
 * base-url / api-key / model-name 全部来自 application.yml（P2：配置外部化），
 * api-key 缺省时回退到环境变量 DEEPSEEK_API_KEY。
 */
@Configuration
public class ChatModel {

    @Bean
    public OpenAiChatModel openAiChatModel(
            @Value("${tutu.deepseek.base-url}") String baseUrl,
            @Value("${tutu.deepseek.api-key}") String apiKey,
            @Value("${tutu.deepseek.model-name}") String modelName) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel(
            @Value("${tutu.deepseek.base-url}") String baseUrl,
            @Value("${tutu.deepseek.api-key}") String apiKey,
            @Value("${tutu.deepseek.model-name}") String modelName) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
