package com.tutu.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LangChain4j 学习示例：RAG（Retrieval-Augmented Generation，检索增强生成）。
 *
 * <p>RAG 三步走：</p>
 * <ol>
 *   <li><b>离线建库</b>：文档加载 → 切分 → 嵌入 → 写入向量存储</li>
 *   <li><b>在线检索</b>：用户问题向量化 → 在向量库中找最相似的 topK 片段</li>
 *   <li><b>注入回答</b>：把检索到的片段塞进 system prompt，让模型基于"已知事实"回答问题</li>
 * </ol>
 *
 * <p>本示例使用<strong>纯 langchain4j-core 内置组件</strong>演示 RAG 完整链路：</p>
 * <ul>
 *   <li>{@code FileSystemDocumentLoader} — 加载文档</li>
 *   <li>{@code DocumentSplitters.recursive()} — 按 token 数切分段落</li>
 *   <li>{@code InMemoryEmbeddingStore} — 内存向量存储（生产可替换为 PGVector / Milvus 等）</li>
 *   <li>{@code EmbeddingStoreContentRetriever} — 把检索结果注入 prompt</li>
 * </ul>
 *
 * <p>注意：DeepSeek 当前不提供 OpenAI 兼容的 embedding 接口，所以本示例用
 * <strong>关键词匹配</strong>作为向量检索的简化替代（仅用于学习演示）。
 * 生产环境建议接入：</p>
 * <ul>
 *   <li>{@code langchain4j-embeddings:all-minilm-l6-v2-q} — 内嵌离线模型</li>
 *   <li>{@code langchain4j-open-ai} 的 {@code OpenAiEmbeddingModel} — 调 OpenAI embedding 接口</li>
 *   <li>{@code langchain4j-qdrant} / {@code langchain4j-pgvector} — 持久化向量存储</li>
 * </ul>
 */
public class
RagExample {

    private final OpenAiChatModel chatModel;

    public RagExample(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // ==================== 路线 A：手写每一步（理解原理） ====================

    /**
     * 手写 RAG 完整链路。
     *
     * @param docPath  文档路径
     * @param question 用户问题
     */
    public String handwrittenRagDemo(String docPath, String question) {
        // Step 1：加载文档
        Document document = FileSystemDocumentLoader.loadDocument(Paths.get(docPath));
        System.out.println("[RAG] 加载文档：" + docPath + "，长度 " + document.text().length() + " 字符");

        // Step 2：切分（每段 300 token，相邻段重叠 30 token）
        List<TextSegment> segments = DocumentSplitters.recursive(300, 30).split(document);
        System.out.println("[RAG] 切分为 " + segments.size() + " 个片段");

        // Step 3：写入存储（生产环境用 EmbeddingStore + EmbeddingModel 做向量化）
        // 这里用简单的关键词匹配模拟向量检索
        List<TextSegment> store = new ArrayList<>(segments);

        // Step 4：检索 top3（简化版：按关键词命中数排序）
        List<TextSegment> matches = findTopK(store, question, 3);
        System.out.println("[RAG] 检索到 " + matches.size() + " 个相关片段");

        // Step 5：拼接上下文 → 发给模型
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个文档问答助手。请基于以下文档片段回答用户问题。\n");
        prompt.append("如果文档中没有相关信息，请如实说\"文档中未找到相关信息\"。\n\n");
        for (int i = 0; i < matches.size(); i++) {
            prompt.append("【片段 %d】\n%s\n\n".formatted(i + 1, matches.get(i).text()));
        }
        prompt.append("用户问题：").append(question);

        return chatModel.chat(prompt.toString());
    }

    /**
     * 简化版检索：按关键词命中数排序（仅用于学习演示，生产请用向量相似度）。
     */
    private List<TextSegment> findTopK(List<TextSegment> segments, String query, int k) {
        String[] keywords = query.toLowerCase().split("\\s+");
        return segments.stream()
                .sorted((a, b) -> Integer.compare(score(b, keywords), score(a, keywords)))
                .limit(k)
                .collect(Collectors.toList());
    }

    private int score(TextSegment segment, String[] keywords) {
        String text = segment.text().toLowerCase();
        int s = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) s++;
        }
        return s;
    }

    // ==================== 路线 B：使用 EmbeddingStoreContentRetriever（推荐） ====================

    /**
     * 使用 LangChain4j 内置的 {@link dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever}
     * 完成检索注入。
     *
     * <p>需要引入 {@code langchain4j-easy-rag} 或自行提供 {@code EmbeddingModel}。</p>
     *
     * <pre>{@code
     * // 生产代码骨架（需要 EmbeddingModel 实现）：
     *
     * EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
     * EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
     *
     * // 离线建库
     * Document doc = FileSystemDocumentLoader.loadDocument(path);
     * List<TextSegment> segments = DocumentSplitters.recursive(300, 30).split(doc);
     * for (TextSegment seg : segments) {
     *     Embedding emb = embeddingModel.embed(seg).content();
     *     store.add(emb, seg);
     * }
     *
     * // 在线检索
     * ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
     *     .embeddingStore(store)
     *     .embeddingModel(embeddingModel)
     *     .maxResults(3)
     *     .build();
     *
     * // 注入 AiService
     * assistant = AiServices.builder(Assistant.class)
     *     .chatModel(chatModel)
     *     .contentRetriever(retriever)
     *     .build();
     * }</pre>
     */
    public String contentRetrieverDemo(String docPath, String question) {
        // 这里仅作演示入口，实际运行需要 EmbeddingModel 实现
        return handwrittenRagDemo(docPath, question);
    }

    // ==================== 独立运行入口 ====================

    /**
     * 独立运行方法：可在 IDE 中直接跑。
     *
     * <p>需要：
     * <ul>
     *   <li>环境变量 DEEPSEEK_API_KEY</li>
     *   <li>在项目根目录放置一个 sample.txt 文件作为知识库</li>
     * </ul>
     * </p>
     */
    public static void main(String[] args) throws IOException {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-v4-flash")
                .build();

        RagExample rag = new RagExample(chatModel);

        // 准备一个示例文档
        Path docPath = Paths.get("rag-sample.txt");
        if (!docPath.toFile().exists()) {
            Files.writeString(docPath, """
                    TuTu 项目技术栈说明

                    TuTu 是一个基于 Spring Boot 4.1 + Java 21 的 AI 教学助手项目。
                    后端使用 LangChain4j 1.3.0 作为 LLM 集成框架，通过 OpenAI 兼容接口对接 DeepSeek 模型。

                    项目结构：
                    - controller 层：ChatController 提供 /api/v1/chat 和 /api/v1/chat/stream 接口
                    - service 层：ChatServiceImpl 封装同步和流式两种调用方式
                    - configuration 层：ChatModel 配置 OpenAiChatModel 和 OpenAiStreamingChatModel

                    核心依赖：
                    - langchain4j-core：提供 AiService、ChatMemory、Tool 等抽象
                    - langchain4j-open-ai：OpenAI 兼容客户端，用于对接 DeepSeek
                    - langchain4j-easy-rag：简化 RAG 实现
                    - langchain4j-embeddings：内嵌向量模型，用于离线 RAG 演示

                    开发团队：caojinbiao
                    启动命令：mvn spring-boot:run -pl tutu-server
                    """);
        }

        String answer = rag.handwrittenRagDemo(docPath.toString(), "TuTu 项目用的什么 LLM 框架？");
        System.out.println("\n=== 回答 ===\n" + answer);
    }
}
