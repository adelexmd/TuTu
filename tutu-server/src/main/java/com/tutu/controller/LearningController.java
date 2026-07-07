package com.tutu.controller;

import com.tutu.assistant.LearningAssistant;
import com.tutu.common.ChatRequest;
import com.tutu.common.Result;
import com.tutu.memory.MemoryDemo;
import com.tutu.rag.RagExample;
import com.tutu.streaming.StreamingDemo;
import com.tutu.structured.StructuredDemo;
import com.tutu.tool.AssistantTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j 学习示例统一入口。
 *
 * <p>挂载在 {@code /api/v1/learn/**}，与现有业务接口 {@code /api/v1/chat/**} 互不干扰。</p>
 *
 * <h3>能力路由</h3>
 * <ul>
 *   <li>{@code POST /api/v1/learn/chat}              — AiService 基础</li>
 *   <li>{@code POST /api/v1/learn/chat-with-memory}  — @MemoryId 多轮记忆</li>
 *   <li>{@code POST /api/v1/learn/tool}              — @Tool 自动工具调用</li>
 *   <li>{@code POST /api/v1/learn/rag}               — RAG 检索增强</li>
 *   <li>{@code POST /api/v1/learn/structured}        — 结构化输出</li>
 *   <li>{@code POST /api/v1/learn/stream}            — TokenStream 流式</li>
 * </ul>
 *
 * <p>统一请求体复用 {@link ChatRequest}：</p>
 * <pre>{@code
 * {
 *   "sessionId": "任意用于会话记忆的标识",
 *   "message": "用户问题",
 *   "mode": "可选，结构化输出时填 actor 名"
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/v1/learn")
public class LearningController {

    private final OpenAiChatModel chatModel;

    private final LearningAssistant baseAssistant;
    private final LearningAssistant memoryAssistant;
    private final MemoryDemo memoryDemo;
    private final RagExample ragExample;
    private final StructuredDemo structuredDemo;
    private final StreamingDemo streamingDemo;

    @Autowired
    public LearningController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;

        // 1. 基础 AiService（无记忆、无工具）
        this.baseAssistant = AiServices.create(LearningAssistant.class, chatModel);

        // 2. 带记忆 AiService（按 sessionId 隔离，最多保留 20 轮）
        this.memoryAssistant = AiServices.builder(LearningAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(sessionId ->
                        MessageWindowChatMemory.withMaxMessages(20))
                .build();

        // 3. 底层 demos
        this.memoryDemo = new MemoryDemo(chatModel);
        this.ragExample = new RagExample(chatModel);
        this.structuredDemo = new StructuredDemo(chatModel);
        this.streamingDemo = new StreamingDemo(chatModel);
    }

    // ---------- 1. AiService 基础 ----------

    /**
     * POST /api/v1/learn/chat
     * 最基础调用：通过 AiService 接口发送消息，框架自动处理 request/reply。
     */
    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody ChatRequest req) {
        String content = baseAssistant.chat(req.getMessage());
        return Result.success(Map.of("content", content));
    }

    /**
     * POST /api/v1/learn/ask
     * 演示命名参数 + UserMessage 模板（区别于 chat 的直接透传）。
     */
    @PostMapping("/ask")
    public Result<Map<String, String>> ask(@RequestBody ChatRequest req) {
        String content = baseAssistant.ask(req.getMessage());
        return Result.success(Map.of("content", content));
    }

    // ---------- 2. 多轮记忆 ----------

    /**
     * POST /api/v1/learn/chat-with-memory
     * 同一 sessionId 的多轮对话会共享 ChatMemory。
     *
     * <p>示例：第一轮"我叫张三"，第二轮"我叫什么"应能答出"张三"。</p>
     */
    @PostMapping("/chat-with-memory")
    public Result<Map<String, String>> chatWithMemory(@RequestBody ChatRequest req) {
        String sessionId = req.getSessionId() == null ? "default" : req.getSessionId();
        String content = memoryAssistant.chatWithMemory(sessionId, req.getMessage());
        return Result.success(Map.of("sessionId", sessionId, "content", content));
    }

    // ---------- 3. Tool Calling ----------

    /**
     * POST /api/v1/learn/tool
     * 演示 @Tool 自动调用：模型判断需要获取外部信息时，自动调用 AssistantTool 中的 Java 方法。
     *
     * <p>请求示例：
     * <ul>
     *   <li>{@code "今天北京天气如何"} → 触发 weather 工具</li>
     *   <li>{@code "订单 ORD-202607001 状态如何"} → 触发 queryOrder 工具</li>
     *   <li>{@code "计算 12.5 * (3 + 7)"} → 触发 calc 工具</li>
     * </ul>
     * </p>
     */
    @PostMapping("/tool")
    public Result<Map<String, Object>> tool(@RequestBody ChatRequest req) {
        // 检测到需要工具时再构建（也可提到构造函数缓存）
        LearningAssistant toolAssistant = AiServices.builder(LearningAssistant.class)
                .chatModel(chatModel)
                .tools(new AssistantTool())   // 注入工具
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String sessionId = req.getSessionId() == null ? "default-tool" : req.getSessionId();
        String content = toolAssistant.chatWithMemory(sessionId, req.getMessage());

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("content", content);
        data.put("availableTools", List.of("weather", "queryOrder", "calc"));
        return Result.success(data);
    }

    // ---------- 4. RAG ----------

    /**
     * POST /api/v1/learn/rag
     * 检索增强生成：从 docs/ 目录加载文档片段，注入到 prompt 中。
     *
     * <p>请求体中 message 即用户问题，无需额外字段。</p>
     */
    @PostMapping("/rag")
    public Result<Map<String, String>> rag(@RequestBody ChatRequest req) {
        // 使用 tutu 项目需求文档作为知识库（路径相对于工作目录）
        java.nio.file.Path docPath = java.nio.file.Paths.get("doc", "tutu需求文档.md");
        if (!docPath.toFile().exists()) {
            return Result.error("文档不存在：" + docPath.toAbsolutePath());
        }

        String answer = ragExample.handwrittenRagDemo(
                docPath.toString(),
                req.getMessage()
        );
        return Result.success(Map.of("content", answer));
    }

    // ---------- 5. 结构化输出 ----------

    /**
     * POST /api/v1/learn/structured
     * 结构化输出：让模型按 schema 返回 Java 对象。
     *
     * <p>请求示例：{@code {"mode":"周星驰"}} 返回 {@code {"actor":"周星驰","films":[...]}}</p>
     */
    @PostMapping("/structured")
    public Result<Map<String, Object>> structured(@RequestBody ChatRequest req) {
        String actor = (req.getMode() == null || req.getMode().isBlank())
                ? "周星驰"
                : req.getMode();
        StructuredDemo.ActorFilms films = structuredDemo.aiServiceDemo(actor, 3);

        Map<String, Object> data = new HashMap<>();
        data.put("actor", films.actor());
        data.put("films", films.films());
        return Result.success(data);
    }

    // ---------- 6. 流式输出 ----------

    /**
     * POST /api/v1/learn/stream  (Content-Type: text/event-stream)
     * TokenStream + SseEmitter，逐 token 推送。
     */
    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody ChatRequest req) {
        return streamingDemo.sseEmitterDemo(req.getMessage());
    }
}
