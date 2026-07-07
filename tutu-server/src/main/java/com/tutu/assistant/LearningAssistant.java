package com.tutu.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.TokenStream;

/**
 * LangChain4j 学习入口：AiService 接口。
 *
 * <p>通过 {@code AiServices.create(LearningAssistant.class, chatModel)} 动态生成实现，
 * 无需手写实现类。这是 LangChain4j 最核心的高层抽象。</p>
 *
 * <p>注解说明：
 * <ul>
 *   <li>{@link SystemMessage} — 系统级 role=system 提示词，可写死也可用 {@code {{占位符}}}</li>
 *   <li>{@link UserMessage}  — role=user 的输入模板，{@code {{...}}} 由命名参数填充</li>
 *   <li>{@link MemoryId}     — 用于做会话隔离，配合 ChatMemoryProvider 实现多轮记忆</li>
 *   <li>{@link V}            — 单个命名参数的简写形式</li>
 * </ul>
 * </p>
 */
public interface LearningAssistant {

    /**
     * 最简模式：一条系统消息 + 用户输入直接透传。
     */
    @SystemMessage("你是一个 Java 教学助手，用简洁的中文回答。")
    String chat(@UserMessage String message);

    /**
     * 命名参数 + 模板化 UserMessage 示例。
     *
     * <p>发起调用时 {@code ask("解释一下 Java 中的 sealed class")} 生成的 UserMessage 为：
     * {@code "请解释：解释一下 Java 中的 sealed class"}</p>
     */
    @SystemMessage("你是一个 Java 教学助手，用简洁的中文回答。")
    String ask(@UserMessage("请解释：{{topic}}") @V("topic") String topic);

    /**
     * 多轮记忆模式：同一个 sessionId 的对话历史会被 ChatMemoryProvider 维护。
     *
     * <p>Bean 创建时需要额外配置 {@code .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))}。</p>
     */
    @SystemMessage("你是一个 Java 教学助手，用简洁的中文回答。记住之前的对话上下文。")
    String chatWithMemory(@MemoryId String sessionId, @UserMessage String message);

    /**
     * 流式版本：返回 TokenStream，比手写 StreamingChatResponseHandler 简洁一倍以上。
     */
    @SystemMessage("你是一个 Java 教学助手，用简洁的中文回答。")
    TokenStream stream(@UserMessage String message);
}
