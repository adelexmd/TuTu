package com.tutu.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface Assistant {

    @SystemMessage("""
            你是一个高级开发工程师。
            你擅长用简练、准确、适合初学者理解的方式解释技术问题。
            回答时不要废话，不要复述规则。
            """)
    @UserMessage("""
            请解释下面这个问题：

            {{topic}}

            回答要求：
            1. 不要超过300字
            2. 先用一句话说明核心结论
            3. 再适当举例说明
            4. 最后用一句话总结
            """)
    String explain(String topic);
}