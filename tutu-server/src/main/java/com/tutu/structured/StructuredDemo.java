package com.tutu.structured;

import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.Result;

import java.util.List;

/**
 * LangChain4j 学习示例：结构化输出（Structured Output）。
 *
 * <p>让 LLM 按指定 schema 返回 JSON，并直接反序列化为 Java 对象，而不是解析字符串。</p>
 *
 * <h3>三种实现方式</h3>
 * <ol>
 *   <li><b>AiService + 返回 Bean</b>（推荐，最简洁）</li>
 *   <li><b>Model.chat + JsonSchema</b>（底层 API）</li>
 *   <li><b>StructuredPrompt</b>（模板化 prompt）</li>
 * </ol>
 */
public class StructuredDemo {

    private final OpenAiChatModel chatModel;

    public StructuredDemo(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // ---------- 1. 用 record 定义 schema ----------

    /**
     * 让模型抽取"演员 → 电影列表"的结构化信息。
     * LangChain4j 会自动生成对应的 JSON Schema 发给模型，并把响应反序列化回来。
     */
    public record ActorFilms(String actor, List<String> films) {}

    /**
     * 让模型给出一个教程计划（嵌套结构演示）。
     */
    public record TutorialPlan(String topic, List<Step> steps) {
        public record Step(int order, String title, String description) {}
    }

    // ---------- 2. AiService 方式（最简洁，推荐） ----------

    private interface StructuredAssistant {
        @UserMessage("列出 {{count}} 部由 {{actor}} 主演的电影。")
        ActorFilms extractFilms(@V("actor") String actor, @V("count") int count);
    }

    public ActorFilms aiServiceDemo(String actor, int count) {
        StructuredAssistant assistant = AiServices.create(StructuredAssistant.class, chatModel);
        return assistant.extractFilms(actor, count);
    }

    // ---------- 3. 直接指定 JsonSchema（底层 API） ----------

    public ActorFilms jsonSchemaDemo(String actor) {
        // 显式声明 schema（比自动推导更可控）
        var itemSchema = JsonObjectSchema.builder()
                .addProperty("actor", dev.langchain4j.model.chat.request.json.JsonStringSchema.builder()
                        .description("演员姓名").build())
                .addProperty("films", dev.langchain4j.model.chat.request.json.JsonArraySchema.builder()
                        .items(dev.langchain4j.model.chat.request.json.JsonStringSchema.builder().build())
                        .description("电影列表").build())
                .required(List.of("actor", "films"))
                .build();
        JsonSchema schema = JsonSchema.builder().name("ActorFilms").rootElement(itemSchema).build();

        String prompt = "列出 %s 主演的 3 部电影。严格按 JSON 返回。".formatted(actor);
        String json = chatModel.chat(prompt); // 实际应该配合 responseFormat 使用，见下方注释

        // 生产代码建议用 OpenAiChatModel.builder().responseFormat("json_object") + 自定义 ObjectMapper 反序列化
        System.out.println("[Structured] 模型返回 JSON：" + json);
        return new ActorFilms(actor, List.of("示例1", "示例2")); // 占位，实际应由反序列化获得
    }

    // ---------- 4. StructuredPrompt 模板 ----------

    public ActorFilms structuredPromptDemo(String query) {
        // 注意：langchain4j 1.x 中 StructuredPrompt 是注解（用于标注 AiServices 接口），
        // 不再提供 StaticPrompt.parse(...) 这种静态工厂。这里仅做模板渲染示意，
        // 真实场景应定义 @StructuredPrompt 标注的接口并交给 AiServices 处理。
        String prompt = """
                从以下文本中抽取"演员-电影"信息，返回 JSON。

                文本：%s
                """.formatted(query);

        // 真实场景中这里接 AiService 或 model.generate(prompt) + JsonSchema
        System.out.println("[Structured] 渲染后的 prompt：" + prompt);
        return new ActorFilms("示例演员", List.of("示例电影1", "示例电影2")); // 占位
    }

    // ==================== 独立运行入口 ====================

    public static void main(String[] args) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-v4-flash")
                .build();

        StructuredDemo demo = new StructuredDemo(model);

        // 1. AiService 方式（最关键的一个）
        ActorFilms films = demo.aiServiceDemo("周星驰", 3);
        System.out.println("== AiService 结构化输出 ==");
        System.out.println("演员：" + films.actor());
        System.out.println("电影：" + films.films());
    }
}
