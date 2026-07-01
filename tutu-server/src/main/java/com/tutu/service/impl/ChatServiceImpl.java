package com.tutu.service.impl;

import com.tutu.service.chatService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements chatService {
    private OpenAiChatModel chatModel;
    @Autowired
    public ChatServiceImpl(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }
@Override
public String explain(String topic) {
    String prompt = """
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
            """.formatted(topic);

    return chatModel.chat(prompt);
}

    @Override
    public String sendMessage(String message) {
        return chatModel.chat(message);
    }
}
