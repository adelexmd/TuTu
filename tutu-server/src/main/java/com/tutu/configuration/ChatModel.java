package com.tutu.configuration;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModel {

    @Bean
    public OpenAiChatModel openAiChatModel() {
         OpenAiChatModel.OpenAiChatModelBuilder  builder = new OpenAiChatModel.OpenAiChatModelBuilder();
            return builder.apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com").modelName("deepseek-v4-flash")
                .build();
    }
}
