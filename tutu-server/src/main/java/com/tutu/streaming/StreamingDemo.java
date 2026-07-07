package com.tutu.streaming;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Consumer;

/**
 * LangChain4j 学习示例：流式输出（Streaming）。
 *
 * <p>演示三种流式写法，从简单到复杂：</p>
 * <ol>
 *   <li>{@link #tokenStreamDemo} — AiService + TokenStream（最简洁，推荐起步）</li>
 *   <li>{@link #chatModelStreamDemo} — ChatModel 底层 API 直接流式</li>
 *   <li>{@link #sseEmitterDemo} — TokenStream 接入 Spring SseEmitter（生产推荐）</li>
 * </ol>
 *
 * <p>对比现有 {@code ChatServiceImpl.stream} 的差异：</p>
 * <ul>
 *   <li>现有：手写 {@code SseEmitter} + 匿名内部类 + try/catch 处理三个回调</li>
 *   <li>推荐：{@code TokenStream} + 链式回调，代码量下降一半以上</li>
 * </ul>
 *
 * <p>LangChain4j 1.3.0 TokenStream API：
 * <ul>
 *   <li>{@code onPartialResponse(Consumer&lt;String&gt;)} — 每段 token</li>
 *   <li>{@code onCompleteResponse(Consumer&lt;ChatResponse&gt;)} — 完整响应</li>
 *   <li>{@code onError(Consumer&lt;Throwable&gt;)} — 异常</li>
 *   <li>{@code start()} — 启动</li>
 * </ul>
 * </p>
 */
public class StreamingDemo {

    private final OpenAiChatModel chatModel;
    private final StreamingAssistant assistant;

    public StreamingDemo(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
        this.assistant = AiServices.create(StreamingAssistant.class, chatModel);
    }

    // ---------- AiService 接口（仅声明，实现由 LangChain4j 在运行时生成） ----------

    private interface StreamingAssistant {
        @SystemMessage("你是一个 Java 教学助手，用简洁的中文回答。")
        TokenStream stream(@UserMessage String message);
    }

    // ==================== 1. 最简洁：TokenStream ====================

    /**
     * TokenStream 链式 API：比手写 StreamingChatResponseHandler 简洁。
     *
     * @param userMessage 用户输入
     * @param onNext      每收到一个 token 的回调（可打印/推 SSE）
     * @param onComplete  完整响应结束回调
     * @param onError     异常回调
     */
    public void tokenStreamDemo(String userMessage,
                                Consumer<String> onNext,
                                Consumer<ChatResponse> onComplete,
                                Consumer<Throwable> onError) {
        assistant.stream(userMessage)
                .onPartialResponse(onNext::accept)
                .onCompleteResponse(onComplete::accept)
                .onError(onError::accept)
                .start();
    }

    // ==================== 2. ChatModel 底层流式（对照参考） ====================

    /**
     * 直接用 ChatModel 底层的 streaming 接口。注意 LangChain4j 1.3.0 中
     * {@code OpenAiChatModel} 同时实现 {@code ChatModel} 和 {@code StreamingChatModel}，
     * 但两者的 {@code chat} 方法是分开的：
     * <ul>
     *   <li>{@code chatModel.chat(String)} -> String（同步，立即返回完整文本）</li>
     *   <li>{@code streamingChatModel.chat(String, handler)}（流式，异步回调）</li>
     * </ul>
     *
     * <p>演示通过向下转型获取 StreamingChatModel 的方式。</p>
     */
    public void chatModelStreamDemo(String userMessage, Consumer<String> onNext) {
        if (!(chatModel instanceof dev.langchain4j.model.chat.StreamingChatModel streaming)) {
            throw new IllegalStateException("当前 ChatModel 不支持流式");
        }
        streaming.chat(userMessage, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                onNext.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                ChatResponseMetadata metadata = response.metadata();
                int outputTokens = metadata.tokenUsage() != null
                        ? metadata.tokenUsage().outputTokenCount() : 0;
                System.out.println("\n[流完成] 输出 token 数：" + outputTokens);
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("[流错误] " + error.getMessage());
            }
        });
    }

    // ==================== 3. TokenStream + SseEmitter（生产写法） ====================

    /**
     * 把 TokenStream 接入 Spring SseEmitter，前端按 SSE 协议接收逐 token 推送。
     *
     * <p>返回 {@link SseEmitter}，超时 30s（与现有 ChatServiceImpl 保持一致）。</p>
     */
    public SseEmitter sseEmitterDemo(String userMessage) {
        SseEmitter emitter = new SseEmitter(30000L);

        assistant.stream(userMessage)
                .onPartialResponse(token -> {
                    try {
                        // SSE 协议：data: {"delta":"..."}
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onCompleteResponse(resp -> {
                    try {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onError(emitter::completeWithError)
                .start();

        return emitter;
    }

    // ==================== 独立运行入口 ====================

    public static void main(String[] args) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-v4-flash")
                .build();

        StreamingDemo demo = new StreamingDemo(model);

        // 演示 1：TokenStream
        System.out.println("=== TokenStream 演示 ===");
        demo.tokenStreamDemo(
                "用一句话解释 Java 的 sealed class",
                token -> System.out.print(token),
                resp -> System.out.println("\n[完成]"),
                err -> System.err.println("[错误] " + err.getMessage())
        );

        // 等待异步流结束
        try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
    }
}
