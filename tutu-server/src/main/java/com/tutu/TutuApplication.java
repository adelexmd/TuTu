package com.tutu;

import com.tutu.controller.LearningController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
// 学习 playground（LearningController 及其 demo 类）在 langchain4j 1.3.0 下有多处运行时装配问题，
// 且与聊天业务无关，这里从组件扫描中排除，保证 /api/v1/chat/** 正常启动。
// 需要启用学习示例时，删除下面这行 excludeFilters 即可（并修复其 @MemoryId / RAG 依赖）。
@ComponentScan(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = LearningController.class))
public class TutuApplication {
    public static void main(String[] args) {
        SpringApplication.run(TutuApplication.class, args);
    }
}
