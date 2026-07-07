# TuTu 项目需求文档

> 版本：v1.0 | 最后更新：2026-07-07

---

## 一、项目概述

### 1.1 项目定位

TuTu 是一个基于大语言模型（LLM）的智能编程教学助手应用，面向编程初学者，提供对话式技术问答和代码解释服务。项目采用前后端分离架构，后端集成 DeepSeek 大模型，前端提供 React 和 Vue 双技术栈实现。

### 1.2 核心目标

1. 提供一个简洁、易用的 AI 对话界面，帮助初学者理解编程概念
2. 支持多样化场景：自由对话、代码解释、学习辅助
3. 通过双前端实现（React + Vue）覆盖不同技术栈学习者的需求

---

## 二、系统架构

### 2.1 项目结构

```
TuTu/
├── tutu-server/          # Spring Boot 后端服务
│   ├── src/main/java/com/tutu/
│   │   ├── TutuApplication.java
│   │   ├── controller/   # REST 控制器层
│   │   ├── service/      # 业务逻辑层
│   │   ├── configuration/ # Spring 配置类
│   │   └── assistant/    # LangChain4j AI 接口
│   └── pom.xml
├── tutu-react/           # React 前端（TypeScript + Vite）
│   └── src/
├── tutu-web/             # Vue 前端（TypeScript + Vite）
│   └── src/
└── doc/                  # 项目文档
```

### 2.2 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 4.1.0 |
| Java | JDK | 21 |
| AI 框架 | LangChain4j | 1.3.0 |
| LLM 服务 | DeepSeek | deepseek-v4-flash |
| React 前端 | React + TypeScript + Vite | 19 / 6.0 / 8.1 |
| Vue 前端 | Vue 3 + TypeScript + Vite | 3.5 / 6.0 / 8.1 |

### 2.3 当前状态

| 模块 | 状态 | 说明 |
|------|------|------|
| 后端 AI 集成 | ✅ 已完成 | 通过 langchain4j 接入 DeepSeek，可正常对话 |
| 后端 REST API | ⚠️ 基础完成 | 有 3 个 GET 接口，但存在冗余和规范问题 |
| React 前端 | ❌ 未集成 | 仅有计数器 demo，未接入后端 API |
| Vue 前端 | ❌ 未集成 | 仅有 Vite 默认模板，未接入后端 API |
| 会话管理 | ❌ 未实现 | 无对话历史、无上下文记忆 |
| 流式响应 | ❌ 未实现 | 当前为阻塞式同步调用 |

---

## 三、功能需求

### 3.1 后端 API 重构

#### 3.1.1 统一对话接口

**问题**：当前 `ChatController` 存在 3 个 GET 端点（`/ai/chat`、`/ai/explain`、`/ai/explain1`），其中 `/explain` 和 `/explain1` 功能完全重复。

**需求**：

- [ ] 3.1.1.1 合并为统一的 POST 接口 `/api/v1/chat`，请求体包含 `message` 和 `mode` 字段
  - `mode=general`：自由对话模式
  - `mode=explain`：代码/概念解释模式
- [ ] 3.1.1.2 统一返回 JSON 结构：`{ "code": 0, "data": { "content": "..." }, "message": "success" }`
- [ ] 3.1.1.3 清理冗余端点和代码

#### 3.1.2 流式响应（SSE）

**问题**：当前为同步阻塞调用，用户需等待完整回复才能看到结果，体验差。

**需求**：

- [ ] 3.1.2.1 新增流式对话接口 `POST /api/v1/chat/stream`，使用 Server-Sent Events（SSE）返回
- [ ] 3.1.2.2 替换 `OpenAiChatModel.chat()` 为 `OpenAiStreamingChatModel`，实现 token 级流式输出
- [ ] 3.1.2.3 SSE 事件格式：
  ```
  data: {"delta": "这是", "finish": false}
  data: {"delta": "一段回复", "finish": false}
  data: {"delta": "", "finish": true}
  ```

#### 3.1.3 会话管理

**问题**：每次请求独立，无上下文记忆，无法进行多轮对话。

**需求**：

- [ ] 3.1.3.1 引入会话（Session）概念，每个会话有唯一 `sessionId`
- [ ] 3.1.3.2 后端使用 `MessageWindowChatMemory`（langchain4j）保存最近 N 轮（默认 10 轮）对话历史
- [ ] 3.1.3.3 新增会话管理接口：
  - `POST /api/v1/session/new` — 创建新会话，返回 sessionId
  - `GET /api/v1/session/{id}/history` — 获取会话历史
  - `DELETE /api/v1/session/{id}` — 清除会话

#### 3.1.4 统一异常处理

- [ ] 3.1.4.1 添加全局异常处理器（`@ControllerAdvice`）
- [ ] 3.1.4.2 AI 服务超时处理（30s 超时，返回友好提示）
- [ ] 3.1.4.3 API Key 缺失时启动即报错，给出明确提示

### 3.2 React 前端（tutu-react）

#### 3.2.1 对话界面

- [ ] 3.2.1.1 聊天界面布局：顶部标题栏 + 中间对话列表区 + 底部输入区
- [ ] 3.2.1.2 对话气泡样式：用户消息靠右（蓝色）、AI 回复靠左（灰色）
- [ ] 3.2.1.3 输入框支持 Enter 发送、Shift+Enter 换行
- [ ] 3.2.1.4 消息列表自动滚动到底部

#### 3.2.2 流式展示

- [ ] 3.2.2.1 使用 EventSource / fetch + ReadableStream 接收 SSE 流
- [ ] 3.2.2.2 AI 回复逐字/逐 token 显示，模拟打字效果
- [ ] 3.2.2.3 流式输出期间显示闪烁光标指示器
- [ ] 3.2.2.4 支持中断生成（停止按钮）

#### 3.2.3 会话功能

- [ ] 3.2.3.1 左侧会话列表：显示历史会话，支持切换
- [ ] 3.2.3.2 "新建对话"按钮
- [ ] 3.2.3.3 删除会话功能

#### 3.2.4 模式切换

- [ ] 3.2.4.1 输入区上方提供模式切换：普通对话 / 代码解释
- [ ] 3.2.4.2 代码解释模式下，输入框提示"请输入要解释的代码或概念"

#### 3.2.5 状态管理

- [ ] 3.2.5.1 loading 状态：发送中显示加载动画，禁用发送按钮
- [ ] 3.2.5.2 error 状态：网络错误/AI 超时时显示错误提示
- [ ] 3.2.5.3 empty 状态：无对话时显示欢迎引导语

#### 3.2.6 UI/UX

- [ ] 3.2.6.1 响应式布局：桌面端三栏 / 移动端单栏
- [ ] 3.2.6.2 深色/浅色模式切换
- [ ] 3.2.6.3 Markdown 渲染支持（代码高亮）

### 3.3 Vue 前端（tutu-web）

**需求与 React 前端完全对齐**（3.2.1 ~ 3.2.6），使用 Vue 3 Composition API 实现：

- [ ] 3.3.1 使用 `fetch` + `ReadableStream` 处理 SSE 流
- [ ] 3.3.2 状态管理使用 Vue `ref`/`reactive`（或 Pinia）
- [ ] 3.3.3 Markdown 渲染使用 `vue-markdown` 或 `marked` + 自定义组件
- [ ] 3.3.4 其余功能点同 3.2.1 ~ 3.2.6

### 3.4 后端配置优化

- [ ] 3.4.1 将 API Key、Base URL、Model Name 等配置项外部化到 `application.yml`
- [ ] 3.4.2 支持多环境配置（dev / prod）
- [ ] 3.4.3 添加 CORS 配置，允许前端开发服务器跨域访问

---

## 四、接口规范

### 4.1 API 一览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/chat` | 同步对话 |
| POST | `/api/v1/chat/stream` | 流式对话（SSE） |
| POST | `/api/v1/session/new` | 创建新会话 |
| GET | `/api/v1/session/{id}/history` | 获取会话历史 |
| DELETE | `/api/v1/session/{id}` | 删除会话 |

### 4.2 请求/响应格式

**POST /api/v1/chat**
```json
// Request
{
  "sessionId": "uuid-string",
  "message": "什么是闭包？",
  "mode": "explain"
}
// Response
{
  "code": 0,
  "data": {
    "sessionId": "uuid-string",
    "content": "闭包是指..."
  },
  "message": "success"
}
```

**统一错误响应**
```json
{
  "code": -1,
  "data": null,
  "message": "AI 服务响应超时，请重试"
}
```

---

## 五、非功能需求

### 5.1 性能

- AI 接口超时时间：30 秒
- 前端首屏加载时间：< 2 秒
- SSE 连接建立时间：< 1 秒

### 5.2 可用性

- 后端服务异常时，前端需展示友好错误提示，而非空白或崩溃
- 网络断开时需有重连提示

### 5.3 可维护性

- 后端代码按 controller / service / configuration 分层，职责清晰
- 前端组件拆分合理，单个组件不超过 200 行
- API 路径统一使用 `/api/v1/` 前缀

---

## 六、实施优先级

| 优先级 | 模块 | 内容 |
|--------|------|------|
| P0 | 后端 | API 重构（统一接口、清理冗余） |
| P0 | 后端 | 流式响应（SSE） |
| P0 | 后端 | CORS 配置 |
| P0 | React 前端 | 基础对话界面 + 流式展示 |
| P1 | 后端 | 会话管理 |
| P1 | React 前端 | 会话列表、新建/切换/删除会话 |
| P1 | React 前端 | 模式切换、Markdown 渲染 |
| P1 | Vue 前端 | 全部功能（跟随 React 版本） |
| P2 | 后端 | 配置外部化、多环境支持 |
| P2 | 双前端 | 深色模式、响应式布局 |

---

## 七、已知问题

1. `ChatController` 中 `/explain` 和 `/explain1` 完全重复，需清理
2. `Assistant` 接口定义了 `explain` 方法，但 `ChatServiceImpl.explain()` 用硬编码的字符串模板绕过了它，`Assistant` bean 实际上未被使用
3. `ChatServiceImpl.sendMessage()` 直接透传用户消息给模型，无任何 prompt 工程处理
4. API Key 通过 `System.getenv()` 读取，无配置外部化
5. 无 CORS 配置，前端开发时会有跨域问题
6. 前端两个项目均未接入后端 API
