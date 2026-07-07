# TuTu 项目记忆

## 定位
基于 LLM 的智能编程教学助手（SpringAI/LangChain4j 学习项目）。前后端分离：Spring Boot 后端 + React/Vue 双前端，后端接 DeepSeek。README 写的 "SpringAI" 与实际用的 LangChain4j 不一致，待改。

## 技术栈
- 后端：Spring Boot 4.1.0 / JDK 21（pom 要求）/ LangChain4j 1.3.0 / DeepSeek (deepseek-v4-flash)
- React：tutu-react（React 19 + TS 6 + Vite 8）
- Vue：tutu-web（Vue 3.5 + TS 6 + Vite 8）
- 需求文档：doc/tutu需求文档.md（含接口规范、已知问题、P0/P1/P2 优先级）

## 后端包结构
com.tutu：controller / service(+impl) / common(Result, SseChunk, ChatRequest, GlobalExceptionHandler) / configuration(ChatModel, CorsConfig) / assistant(已删除)

## 当前状态（2026-07-07）
后端 P0 已完成：POST /api/v1/chat（同步，mode=general|explain）、POST /api/v1/chat/stream（SSE 流式）、CORS、全局异常处理、统一返回结构。会话管理(P1)、配置外部化(P2)、前端接入仍未做。

## 关键坑（LangChain4j 1.3.0）
- 流式不是 `generate`，是 `streamingChatModel.chat(String, StreamingChatResponseHandler)`
- handler 类型：`dev.langchain4j.model.chat.response.StreamingChatResponseHandler`（新包，旧版 dev.langchain4j.model.output.StreamingResponseHandler 已不适用）
- 回调：onPartialResponse(String token) / onCompleteResponse(ChatResponse) / onError(Throwable)
- AiMessage、Response 在 1.x 多位于 dev.langchain4j.data.message / dev.langchain4j.model.chat.response

## 环境注意
- 本机无 JDK 21，最高 JDK 17（D:\software\develop\jdk\jdk-17.0.16）。pom java.version=21 保留，正式构建需 JDK21。
- Maven 3.6.3 在 Git Bash 下调 sh 脚本会报 classworlds 找不到；须用 Windows 路径 + `mvn.cmd`：
  `JAVA_HOME='D:\...\jdk-17.0.16' MAVEN_HOME='D:\...\apache-maven-3.6.3' /d/.../bin/mvn.cmd compile -Dmaven.compiler.release=17`
- 本地仓库：D:\software\develop\Maven\repository（settings.xml 指定）
- PowerShell / Bash 工具内禁止 `cmd.exe`，`.cmd` 文件只能从 Git Bash 直接调用
