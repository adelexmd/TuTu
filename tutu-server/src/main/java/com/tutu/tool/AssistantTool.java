package com.tutu.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LangChain4j 学习示例：@Tool 注解演示。
 *
 * <p>用 {@code @Tool} 标注的方法会被注册到模型的工具列表中。当模型判断需要调用工具时，
 * LangChain4j 会自动发起"工具调用 → 拿回结果 → 再喂给模型"的多轮交互，整个过程对调用方透明。</p>
 *
 * <p>注册方式（在构建 AiService 时）：</p>
 * <pre>{@code
 * LearningAssistant assistant = AiServices.builder(LearningAssistant.class)
 *         .chatModel(chatModel)
 *         .tools(new AssistantTool())          // 注入工具
 *         .build();
 * }</pre>
 *
 * <p>注意：
 * <ul>
 *   <li>{@code @Tool} 里的描述就是告诉模型"这个工具能做什么"，写得越清晰模型越会正确调用</li>
 *   <li>{@code @P} 描述参数含义，帮助模型填充参数</li>
 *   <li>工具方法必须是 public，放在 Spring Bean 里或普通对象里均可</li>
 * </ul>
 * </p>
 */
@Component
public class AssistantTool {

    // ---------- 模拟数据 ----------
    private static final Map<String, String> MOCK_WEATHER = new HashMap<>();
    private static final Map<String, OrderStatus> MOCK_ORDERS = new HashMap<>();

    static {
        MOCK_WEATHER.put("北京", "晴，气温 18°C，微风");
        MOCK_WEATHER.put("上海", "多云，气温 22°C，东南风 3 级");
        MOCK_WEATHER.put("深圳", "雷阵雨，气温 28°C，湿度 85%");
        MOCK_WEATHER.put("杭州", "阴，气温 20°C，东风 2 级");

        MOCK_ORDERS.put("ORD-202607001", new OrderStatus("ORD-202607001", "已发货", "2026-07-05"));
        MOCK_ORDERS.put("ORD-202607002", new OrderStatus("ORD-202607002", "拣货中", "2026-07-06"));
        MOCK_ORDERS.put("ORD-202607003", new OrderStatus("ORD-202607003", "已完成", "2026-07-01"));
    }

    // ---------- 工具方法 ----------

    /**
     * 模拟查询天气。生产环境替换为真实气象接口即可。
     */
    @Tool("查询指定城市的当前天气，返回温度、天气现象与风力信息。如果城市不存在则告知不支持。")
    public String weather(@P("城市名，例如 北京、上海、深圳") String city) {
        System.out.println("[Tool 被调用] weather(city=" + city + ")");
        return MOCK_WEATHER.getOrDefault(city,
                "暂不支持查询\"%s\"的天气，目前仅支持：%s".formatted(city, MOCK_WEATHER.keySet()));
    }

    /**
     * 模拟订单状态查询。
     */
    @Tool("根据订单号查询订单当前状态，返回订单号、状态和下单日期。")
    public OrderStatus queryOrder(@P("订单号，例如 ORD-202607001") String orderId) {
        System.out.println("[Tool 被调用] queryOrder(orderId=" + orderId + ")");
        OrderStatus status = MOCK_ORDERS.get(orderId);
        if (status == null) {
            return new OrderStatus(orderId, "不存在", "-");
        }
        return status;
    }

    /**
     * 模拟计算器。演示工具可以返回 double 等基础类型（会自动装箱成 String 反馈模型）。
     */
    @Tool("计算一个算术表达式并返回数值结果，支持 +、-、*、/ 和括号。例如 '12.5 * (3 + 7)'。")
    public double calc(@P("算术表达式") String expr) {
        System.out.println("[Tool 被调用] calc(expr=" + expr + ")");
        // 极简实现：用 JavaScript 引擎做求值，生产环境请改为 exp4j / Javaluator 等库
        try {
            javax.script.ScriptEngine engine =
                    new javax.script.ScriptEngineManager().getEngineByName("JavaScript");
            Object result = engine.eval(expr);
            return ((Number) result).doubleValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("无法计算表达式：%s（%s）".formatted(expr, e.getMessage()));
        }
    }

    // ---------- 内部 DTO ----------

    public record OrderStatus(String orderId, String status, String orderDate) {
        @Override
        public String toString() {
            return "订单[%s] 状态：%s，下单时间：%s".formatted(orderId, status, orderDate);
        }
    }
}
