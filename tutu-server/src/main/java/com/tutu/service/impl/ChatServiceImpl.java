package com.tutu.service.impl;

import com.tutu.common.SseChunk;
import com.tutu.service.chatService;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatServiceImpl implements chatService {

    private final OpenAiChatModel chatModel;
    private final OpenAiStreamingChatModel streamingChatModel;

    @Autowired
    public ChatServiceImpl(OpenAiChatModel chatModel, OpenAiStreamingChatModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    @Override
    public String sendMessage(String message, String mode) {
        return chatModel.chat(buildPrompt(message, mode));
    }

    @Override
    public SseEmitter stream(String message, String mode) {
        // 30s 超时，对应需求文档非功能需求
        SseEmitter emitter = new SseEmitter(30000L);
        String prompt = buildPrompt(message, mode);

        streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                try {
                    emitter.send(SseEmitter.event().data(new SseChunk(token, false)));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                try {
                    emitter.send(SseEmitter.event().data(new SseChunk("", true)));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        });

        return emitter;
    }

    private String buildPrompt(String message, String mode) {
        if ("explain".equalsIgnoreCase(mode)) {
            return """
                    你是一个高级开发工程师，请直接回答用户的问题。

                    用户问题：
                    %s

                    回答要求：
                    1. 不要超过300字
                    2. 先用一句话说明核心结论
                    3. 再适当举例说明
                    4. 最后用一句话总结
                    5. 不要说“我会按照要求”
                    6. 不要复述这些规则，直接回答问题
                    """.formatted(message);
        }
        return message;
    }
}
