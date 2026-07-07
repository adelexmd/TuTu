package com.tutu.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * LangChain4j 学习示例：ChatMemory 多轮记忆。
 *
 * <p>本示例以<strong>纯模型调用 + 手写记忆</strong>方式演示两种 ChatMemory 的使用模式，
 * 不依赖 AiService 框架，便于理解底层机制。</p>
 *
 * <p>实际项目中推荐通过 AiService 的 {@code @MemoryId} + {@code ChatMemoryProvider}
 * 自动注入（见 LearningAssistant.chatWithMemory），本文件仅供学习参考。</p>
 *
 * <h3>两种常用实现</h3>
 * <ul>
 *   <li>{@link MessageWindowChatMemory} — 按消息条数滑动窗口，简单直观</li>
 *   <li>{@link TokenWindowChatMemory} — 按 token 数截断，更接近 LLM 真实上下文限制</li>
 * </ul>
 */
public class MemoryDemo {

    private final OpenAiChatModel chatModel;

    public MemoryDemo(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 演示 MessageWindowChatMemory：最多保留最近 N 条消息。
     */
    public String chatWithMessageWindow(String sessionId, String userMessage, int maxMessages) {
        // 1. 为该会话创建记忆（生产中应由 Map<String, ChatMemory> 缓存，每次按需取出）
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(maxMessages);

        // 2. 加入用户输入
        memory.add(UserMessage.from(userMessage));

        // 3. 构造 ChatRequest：把记忆中的全部历史 + 当前输入一次性发给模型
        List<ChatMessage> messages = new ArrayList<>(memory.messages());
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder().build())
                .build();
        ChatResponse response = chatModel.chat(request);

        // 4. 把模型回复也存入记忆，供下一轮使用
        memory.add(response.aiMessage());

        System.out.println("[Memory] sessionId=%s 当前记忆条数：%d / 上限 %d"
                .formatted(sessionId, memory.messages().size(), maxMessages));
        return response.aiMessage().text();
    }

    /**
     * 演示 TokenWindowChatMemory：按 token 数截断历史。
     *
     * <p>需要传入一个用于估算 token 数的 {@link dev.langchain4j.model.Tokenizer}。
     * 为简化演示这里用 {@code null}（不走 tokenizer 时会用 char/4 粗略估算）。</p>
     */
    public String chatWithTokenWindow(String sessionId, String userMessage, int maxTokens) {
        ChatMemory memory = TokenWindowChatMemory.withMaxTokens(maxTokens, null);

        memory.add(UserMessage.from(userMessage));

        // 模拟历史已经有一些内容（演示截断效果）
        for (int i = 1; i <= 3; i++) {
            memory.add(UserMessage.from("历史消息 #" + i + "：这是一段占位的历史对话，用于演示 token 截断。"));
            memory.add(AiMessage.from("历史回复 #" + i));
        }
        memory.add(UserMessage.from(userMessage));

        List<ChatMessage> messages = new ArrayList<>(memory.messages());
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder().build())
                .build();
        ChatResponse response = chatModel.chat(request);

        memory.add(response.aiMessage());
        System.out.println("[Memory] sessionId=%s 最终被保留的消息条数：%d（按 token 截断后）"
                .formatted(sessionId, memory.messages().size()));
        return response.aiMessage().text();
    }

    /**
     * 独立运行方法：可在 IDE 中直接跑。需设置环境变量 DEEPSEEK_API_KEY。
     */
    public static void main(String[] args) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-v4-flash")
                .build();

        MemoryDemo demo = new MemoryDemo(model);

        // 第一轮
        String r1 = demo.chatWithMessageWindow("demo-user", "我叫张三，记住我的名字", 10);
        System.out.println("第一轮回复：" + r1);

        // 第二轮：期望模型还记得"张三"
        String r2 = demo.chatWithMessageWindow("demo-user", "我叫什么名字？", 10);
        System.out.println("第二轮回复：" + r2);
    }
}
