package com.tutu.configuration;

import com.tutu.assistant.Assistant;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfig {

    @Bean
    public Assistant assistant(OpenAiChatModel openAiChatModel) {
        return AiServices.create(Assistant.class, openAiChatModel);
    }
}