package com.tutu.service.impl;

import com.tutu.common.SseChunk;
import com.tutu.service.chatService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatServiceImpl implements chatService {

    private static final String SYSTEM_PROMPT =
            "你是一个耐心、专业的编程教学助手，主要面向编程初学者。" +
            "请用通俗易懂的中文回答，必要时给出可运行的代码示例。";

    private final OpenAiChatModel chatModel;
    private final OpenAiStreamingChatModel streamingChatModel;
    private final int maxMessages;

    // 会话记忆：sessionId -> 该会话的 MessageWindowChatMemory（P1：会话管理）
    private final Map<String, MessageWindowChatMemory> memories = new ConcurrentHashMap<>();

    @Autowired
    public ChatServiceImpl(OpenAiChatModel chatModel,
                           OpenAiStreamingChatModel streamingChatModel,
                           @Value("${tutu.memory.max-messages:20}") int maxMessages) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.maxMessages = maxMessages;
    }

    @Override
    public String sendMessage(String sessionId, String message, String mode) {
        MessageWindowChatMemory memory = getOrCreateMemory(sessionId);
        UserMessage userMessage = UserMessage.from(buildUserPrompt(message, mode));
        memory.add(userMessage);
        ChatResponse response = chatModel.chat(memory.messages());
        memory.add(response.aiMessage());
        return response.aiMessage().text();
    }

    @Override
    public SseEmitter stream(String sessionId, String message, String mode) {
        // 流式生成可能较长，超时放宽到 120s
        SseEmitter emitter = new SseEmitter(120000L);
        MessageWindowChatMemory memory = getOrCreateMemory(sessionId);
        UserMessage userMessage = UserMessage.from(buildUserPrompt(message, mode));
        memory.add(userMessage);

        streamingChatModel.chat(memory.messages(), new StreamingChatResponseHandler() {
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
                    memory.add(response.aiMessage());
                    emitter.send(SseEmitter.event().data(new SseChunk("", true, sessionId)));
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

    private MessageWindowChatMemory getOrCreateMemory(String sessionId) {
        return memories.computeIfAbsent(sessionId, id -> {
            MessageWindowChatMemory mem = MessageWindowChatMemory.builder()
                    .id(id)
                    .maxMessages(maxMessages)
                    .build();
            mem.add(SystemMessage.from(SYSTEM_PROMPT));
            return mem;
        });
    }

    private String buildUserPrompt(String message, String mode) {
        if ("explain".equalsIgnoreCase(mode)) {
            return "请详细解释下面的编程概念或代码：先给出核心结论，再用通俗的例子说明，最后用一句话总结。\n\n" + message;
        }
        return message;
    }
}
