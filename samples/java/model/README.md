# A2A Java Models

本模块包含从 Go 语言模型翻译而来的 Java record 类，用于 A2A（Agent-to-Agent）协议数据模型定义。

## 翻译概述

所有 Go 结构体已被翻译为 Java record，保留了原有注释和结构定义。主要翻译规则如下：

### 类型映射

- Go `string` → Java `String`
- Go `*string` → Java `String`（可选字段）
- Go `int` → Java `int`
- Go `*int` → Java `Integer`（可选字段）
- Go `bool` → Java `boolean`
- Go `*bool` → Java `Boolean`（可选字段）
- Go `[]Type` → Java `List<Type>`
- Go `map[string]interface{}` → Java `Map<String, Object>`
- Go `interface{}` → Java `Object`

### 枚举类型

- `TaskState` - 任务状态枚举
- `ErrorCode` - 错误码枚举

### 核心模型

#### 文件内容相关
- `FileContentBase` - 文件内容基础结构
- `FileContent` - 文件内容接口（密封接口）
- `FileContentBytes` - 基于字节的文件内容
- `FileContentURI` - 基于URI的文件内容

#### 消息与任务相关
- `Part` - 消息或产物的组成部分
- `Artifact` - 任务输出或中间文件
- `Message` - A2A 协议中的消息
- `Task` - A2A 任务
- `TaskStatus` - 任务状态
- `TaskHistory` - 任务历史

#### 事件相关
- `TaskStatusUpdateEvent` - 任务状态更新事件
- `TaskArtifactUpdateEvent` - 任务产物更新事件

#### Agent 相关
- `AgentAuthentication` - Agent 认证信息
- `AgentCapabilities` - Agent 能力描述
- `AgentProvider` - Agent 提供方信息
- `AgentSkill` - Agent 技能定义
- `AgentCard` - Agent 元数据卡片

#### JSON-RPC 相关
- `JSONRPCMessageIdentifier` - JSON-RPC 消息标识符
- `JSONRPCMessage` - JSON-RPC 消息基础结构
- `JSONRPCRequest` - JSON-RPC 请求
- `JSONRPCError` - JSON-RPC 错误
- `JSONRPCResponse` - JSON-RPC 响应

#### 请求参数
- `TaskSendParams` - 发送任务消息的参数
- `TaskIDParams` - 基于任务ID的操作参数
- `TaskQueryParams` - 查询任务信息的参数
- `PushNotificationConfig` - 推送通知配置
- `TaskPushNotificationConfig` - 任务专用推送通知配置

#### 具体请求类
- `SendTaskRequest` - 发送任务请求
- `GetTaskRequest` - 获取任务状态请求
- `CancelTaskRequest` - 取消任务请求
- `SetTaskPushNotificationRequest` - 设置任务通知请求
- `GetTaskPushNotificationRequest` - 获取任务通知配置请求
- `TaskResubscriptionRequest` - 重新订阅任务更新请求
- `SendTaskStreamingRequest` - 发送任务流式请求

#### 响应类
- `A2AError` - A2A 协议错误
- `SendTaskResponse` - 发送任务响应
- `SendTaskStreamingResponse` - 任务流式响应
- `GetTaskResponse` - 获取任务响应
- `CancelTaskResponse` - 取消任务响应
- `GetTaskHistoryResponse` - 获取任务历史响应
- `SetTaskPushNotificationResponse` - 设置任务推送通知响应
- `GetTaskPushNotificationResponse` - 获取任务推送通知响应

## Jackson 注解

所有 record 使用 Jackson 注解进行 JSON 序列化/反序列化：

- `@JsonProperty` - 指定 JSON 字段名
- `@JsonInclude(JsonInclude.Include.NON_NULL)` - 排除值为 null 的字段
- `@JsonValue` - 用于枚举类型值序列化
- `@JsonTypeInfo` 和 `@JsonSubTypes` - 用于多态类型处理

## 依赖

本项目使用 Jackson 2.15.2 进行 JSON 处理：

- `jackson-core`
- `jackson-annotations`
- `jackson-databind`

## 编译

```bash
mvn compile
```

所有 Java record 均已通过编译验证，以确保类型安全和正确性。 