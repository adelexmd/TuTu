# TuTu 项目介绍

> Spring AI 2.0 学习项目 —— 基于 Spring Boot + LangChain4j + DeepSeek 的 AI 聊天应用

## 一、项目简介

TuTu 是一个用于学习 **Spring AI 2.0** 与 **LangChain4j** 的实践项目。后端通过 LangChain4j 接入 DeepSeek 大模型，对外提供 AI 对话与技术问题解释接口；前端提供 React 与 Vue 两套学习用页面。

项目采用前后端分离架构，包含三个模块：

| 模块 | 说明 | 技术栈 |
|------|------|--------|
| `tutu-server` | 后端服务 | Spring Boot 4.1.0、LangChain4j 1.3.0、Java 21 |
| `tutu-react` | React 前端 | React 19、TypeScript 6、Vite 8 |
| `tutu-web` | Vue 前端 | Vue 3、TypeScript、Vite |

## 二、技术栈

### 后端

- **JDK**：21
- **框架**：Spring Boot 4.1.0
- **AI 框架**：LangChain4j 1.3.0（`langchain4j` + `langchain4j-open-ai`）
- **大模型**：DeepSeek（`deepseek-v4-flash`，OpenAI 兼容接口）
- **构建工具**：Maven

### 前端

- **tutu-react**：React 19 + TypeScript + Vite 8（使用 oxlint）
- **tutu-web**：Vue 3 + TypeScript + Vite

## 三、目录结构

```
TuTu/
├── README.md
├── tutu-server/                      # 后端服务
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/tutu/
│       │   ├── TutuApplication.java          # 启动类
│       │   ├── controller/
│       │   │   └── ChatController.java       # 对话接口
│       │   ├── service/
│       │   │   ├── chatService.java          # 服务接口
│       │   │   └── impl/ChatServiceImpl.java # 服务实现
│       │   ├── assistant/
│       │   │   └── Assistant.java            # AI Services 声明式接口
│       │   └── configuration/
│       │       ├── ChatModel.java            # OpenAiChatModel 配置
│       │       └── AssistantConfig.java      # Assistant Bean 配置
│       └── resources/
│           └── application.yml               # 服务端口 8899
├── tutu-react/                       # React 前端
│   └── src/
│       ├── main.tsx
│       ├── App.tsx                           # 计数器学习页面
│       └── assets/
└── tutu-web/                         # Vue 前端
    └── src/
        ├── main.ts
        ├── App.vue
        └── components/HelloWorld.vue
```

## 四、核心功能与接口

后端通过 `ChatController` 暴露以下接口（前缀 `/ai`）：

| 接口 | 方法 | 参数 | 说明 |
|------|------|------|------|
| `/ai/chat` | GET | `message` | 直接与 DeepSeek 对话 |
| `/ai/explain` | GET | `message` | 用「高级开发工程师」角色解释技术问题（限 300 字） |
| `/ai/explain1` | GET | `message` | 同 `/ai/explain`（重复接口） |

### AI 调用方式

项目演示了 LangChain4j 的两种调用方式：

1. **直接调用模型**（`ChatServiceImpl`）
   通过 `OpenAiChatModel.chat(prompt)` 直接发送提示词，返回结果。

2. **声明式 AI Services**（`Assistant` 接口 + `AssistantConfig`）
   使用 `@SystemMessage` / `@UserMessage` 注解定义提示词模板，由 `AiServices.create(Assistant.class, openAiChatModel)` 自动生成实现类。

## 五、配置说明

### 环境变量

后端通过环境变量注入 DeepSeek 的 API Key：

```
DEEPSEEK_API_KEY=你的_deepseek_api_key
```

### 模型配置

`ChatModel.java` 中配置 DeepSeek 模型：

- **Base URL**：`https://api.deepseek.com`
- **模型名**：`deepseek-v4-flash`

### 服务端口

`application.yml`：

```yaml
server:
  port: 8899
```

## 六、快速开始

### 1. 准备环境

- JDK 21+
- Maven 3.9+
- Node.js 18+
- 申请 [DeepSeek API Key](https://platform.deepseek.com/)

### 2. 启动后端

```bash
# 设置环境变量
export DEEPSEEK_API_KEY=你的_api_key

# 进入后端目录
cd tutu-server

# 编译并运行
mvn spring-boot:run
```

后端启动后监听 `http://localhost:8899`。

### 3. 启动前端（二选一）

**React 版：**

```bash
cd tutu-react
npm install
npm run dev
```

**Vue 版：**

```bash
cd tutu-web
npm install
npm run dev
```

### 4. 测试接口

```bash
# 直接对话
curl "http://localhost:8899/ai/chat?message=你好"

# 解释技术问题
curl "http://localhost:8899/ai/explain?message=什么是依赖注入"
```

## 七、学习要点

本项目涵盖以下 Spring AI / LangChain4j 学习内容：

1. **LangChain4j 集成 Spring Boot** —— 通过 `@Configuration` + `@Bean` 注入 `OpenAiChatModel`
2. **OpenAI 兼容接口** —— DeepSeek 使用 OpenAI 协议，可直接用 `langchain4j-open-ai` 接入
3. **AI Services 声明式调用** —— 用接口 + 注解定义提示词，类似 Feign 风格
4. **提示词工程** —— `@SystemMessage` 定义角色，`@UserMessage` 定义用户输入模板与约束
5. **环境变量管理密钥** —— 避免硬编码 API Key

## 八、待改进

- `/ai/explain1` 与 `/ai/explain` 重复，可清理
- 前端尚未对接后端 AI 接口（目前仅为计数器学习页面）
- 缺少流式输出（Streaming）支持
- 缺少对话记忆（Chat Memory）与上下文管理
- `Assistant` 接口已定义但未在 Controller 中使用
- 接口类名 `chatService` 不符合 Java 大驼峰命名规范
