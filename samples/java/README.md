# A2A Java 示例项目

本项目是 Agent-to-Agent (A2A) 协议的 Java 实现示例，提供了完整的客户端和服务器端 SDK，以及一个 AI 驱动的翻译服务演示应用程序。

## 项目架构

本项目采用 Maven 多模块架构，包含以下三个核心模块：

```
samples/java/
├── model/          # A2A 协议数据模型
├── server/         # A2A 服务器 SDK 和翻译服务
├── client/         # A2A 客户端 SDK 和示例代码
└── pom.xml         # 父 Maven 配置文件
```

### 模块详情

#### 🎯 **模型模块** (`model/`)
A2A 协议的核心数据模型，为 JSON-RPC 2.0 和 A2A 协议提供完整的数据结构：

- **消息模型**: `Message`, `Part`, `TextPart`, `Artifact`
- **任务模型**: `Task`, `TaskStatus`, `TaskState`
- **Agent 模型**: `AgentCard`, `AgentCapabilities`, `AgentSkill`
- **JSON-RPC 模型**: `JSONRPCRequest`, `JSONRPCResponse`, `JSONRPCError`
- **事件模型**: `TaskStatusUpdateEvent`, `TaskArtifactUpdateEvent`

#### 🚀 **服务器模块** (`server/`)
基于 Spring Boot 的 A2A 服务器 SDK，集成了 Spring AI 框架：

- **核心组件**:
  - `A2AServer`: 管理 Agent 行为的主服务器类
  - `A2AController`: 实现 A2A 协议端点的 REST 控制器
  - `TaskHandler`: 任务处理接口
  - `A2AServerConfiguration`: AI 翻译机器人配置

- **主要特性**:
  - 完整的 JSON-RPC 2.0 支持
  - Agent Card 发布 (`/.well-known/agent-card`)
  - 任务管理 (发送、查询、取消)
  - 流式响应支持 (Server-Sent Events)
  - Spring AI 集成，支持 OpenAI 和其他模型

#### 📱 **客户端模块** (`client/`)
纯 Java A2A 客户端 SDK，包含翻译客户端示例：

- **核心组件**:
  - `A2AClient`: 处理所有 A2A 操作的主客户端类
  - `StreamingEventListener`: 流式事件监听器接口
  - `A2AClientException`: A2A 特定的异常处理
  - `A2AClientExample`: 完整的翻译客户端示例

- **主要特性**:
  - JSON-RPC 2.0 客户端实现
  - Agent 发现和能力查询
  - 同步/异步任务操作
  - 流式响应处理
  - 连接池和错误恢复

## 核心功能实现

### 🤖 AI 翻译服务

项目实现了一个智能翻译 Agent，支持多语言翻译：

**翻译逻辑**:
- 中文 → 英文
- 英文 → 中文
- 其他语言 → 英文

**技术特性**:
- 基于 Spring AI ChatClient
- 支持 OpenAI、Azure OpenAI 和其他模型
- 上下文感知的自然语言翻译
- 实时流式响应

### 🔄 A2A 协议实现

完整实现 A2A 协议规范：

**核心操作**:
- `tasks/send`: 发送任务消息
- `tasks/get`: 查询任务状态
- `tasks/cancel`: 取消任务执行

**协议特性**:
- JSON-RPC 2.0 通信
- Agent 能力发现
- 任务状态跟踪
- 流式事件推送
- 标准化错误码

### 📡 通信机制

**同步通信**:
- HTTP POST `/a2a` - 标准 JSON-RPC 请求
- HTTP GET `/.well-known/agent-card` - Agent 信息检索

**流式通信**:
- HTTP POST `/a2a/stream` - Server-Sent Events
- 实时任务状态更新
- 自动重新连接和错误恢复

## 如何运行

### 要求

- **Java**: 17 或更高版本

### 步骤 1: 编译项目

在项目根目录执行编译：

```bash
cd samples/java
./mvnw clean install -DskipTests
```

### 步骤 2: 配置环境变量

设置 AI 模型相关的环境变量 (翻译功能需要)：

```bash
# OpenAI 配置
export OPENAI_API_KEY="your-openai-api-key"
export OPENAI_BASE_URL="https://api.openai.com"
export OPENAI_CHAT_MODEL="gpt-4o"

# 或者 GCP OpenAI 配置
export OPENAI_API_KEY="your-gcp-api-key"
export OPENAI_BASE_URL="https://{location}-aiplatform.googleapis.com/v1/projects/{project_id}/locations/{location}/endpoints/openapi"
export OPENAI_CHAT_MODEL="gemini-2.5-pro-preview-05-06"
```

### 步骤 3: 启动翻译服务器

启动 A2A 翻译服务器：

```bash
cd server
../mvnw spring-boot:run
```

服务器将在 `http://localhost:8080` 启动，提供以下端点：
- `http://localhost:8080/.well-known/agent-card` - Agent 信息
- `http://localhost:8080/a2a` - A2A 协议端点
- `http://localhost:8080/a2a/stream` - 流式端点

### 步骤 4: 运行翻译客户端

在新的终端窗口中，运行客户端示例：

```bash
cd client
../mvnw exec:java -Dexec.mainClass="com.google.a2a.client.A2AClientExample"
```

## API 使用示例

### 获取 Agent 信息

```bash
curl -X GET http://localhost:8080/.well-known/agent-card \
  -H "Accept: application/json"
```

### 发送翻译任务

```bash
curl -X POST http://localhost:8080/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "request-1",
    "method": "tasks/send",
    "params": {
      "id": "translation-task-1",
      "message": {
        "messageId": "msg-1",
        "kind": "message",
        "role": "user",
        "parts": [
          {
            "kind": "text",
            "text": "Hello, world!"
          }
        ]
      }
    }
  }'
```

### 流式翻译

```bash
curl -X POST http://localhost:8080/a2a/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "id": "stream-request-1",
    "method": "tasks/send",
    "params": {
      "id": "streaming-translation-task",
      "message": {
        "messageId": "stream-msg-1",
        "kind": "message",
        "role": "user",
        "parts": [
          {
            "kind": "text",
            "text": "Hello world!"
          }
        ]
      }
    }
  }'
```

## 扩展开发

### 添加新的 Agent 技能

1. 在 `A2AServerConfiguration` 中定义新的 `AgentSkill`
2. 实现相应的 `TaskHandler` 逻辑
3. 更新 Agent Card 的技能列表
