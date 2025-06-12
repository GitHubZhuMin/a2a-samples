package com.google.a2a.server;

import com.google.a2a.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A2AServer represents an A2A server instance
 * A2AServer 表示一个A2A服务器实例，负责任务的管理和处理
 */
public class A2AServer {

    private final AgentCard agentCard; // 代理卡片，描述AI代理能力
    private final TaskHandler handler; // 任务处理器，处理具体业务逻辑
    private final Map<String, Task> taskStore; // 任务存储，保存所有任务对象
    private final Map<String, List<Message>> taskHistory; // 任务历史，保存每个任务的消息历史
    private final ObjectMapper objectMapper; // JSON对象映射器

    public A2AServer(AgentCard agentCard, TaskHandler handler, ObjectMapper objectMapper) {
        this.agentCard = agentCard;
        this.handler = handler;
        this.taskStore = new ConcurrentHashMap<>();
        this.taskHistory = new ConcurrentHashMap<>();
        this.objectMapper = objectMapper;
    }

    /**
     * Handle task send request
     * 处理任务发送请求，创建新任务并调用业务处理逻辑
     */
    public JSONRPCResponse handleTaskSend(JSONRPCRequest request) {
        try {
            TaskSendParams params = parseParams(request.params(), TaskSendParams.class);

            // Generate contextId if not provided
            // 生成上下文ID（如未提供）
            String contextId = UUID.randomUUID().toString();

            // Create initial task status
            // 创建初始任务状态，标记为WORKING
            TaskStatus initialStatus = new TaskStatus(
                TaskState.WORKING,
                null,  // No message initially
                Instant.now().toString()  // Current timestamp
            );

            // Create new task with all required fields
            // 创建新任务对象，包含所有必要字段
            Task task = new Task(
                params.id(),
                contextId,
                "task",  // kind is always "task"
                initialStatus,
                null,    // No artifacts initially
                null,    // No history initially 
                params.metadata()  // Use metadata from params
            );

            // Process task
            // 调用任务处理器处理任务
            Task updatedTask = handler.handle(task, params.message());

            // Store task and history
            // 存储任务和消息历史
            taskStore.put(task.id(), updatedTask);
            taskHistory.computeIfAbsent(task.id(), k -> new CopyOnWriteArrayList<>())
                      .add(params.message());

            // 返回成功响应，包含处理后的任务
            return createSuccessResponse(request.id(), updatedTask);

        } catch (Exception e) {
            // 发生异常，返回错误响应
            return createErrorResponse(request.id(), ErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Handle task query request
     * 处理任务查询请求，根据ID获取任务及其历史
     */
    public JSONRPCResponse handleTaskGet(JSONRPCRequest request) {
        try {
            TaskQueryParams params = parseParams(request.params(), TaskQueryParams.class);

            Task task = taskStore.get(params.id());
            if (task == null) {
                // 未找到任务，返回错误
                return createErrorResponse(request.id(), ErrorCode.TASK_NOT_FOUND, "Task not found");
            }

            // Include history if requested
            // 如果请求指定了historyLength，则返回部分历史
            if (params.historyLength() != null && params.historyLength() > 0) {
                List<Message> history = getTaskHistory(params.id());
                int limit = Math.min(params.historyLength(), history.size());
                List<Message> limitedHistory = history.subList(Math.max(0, history.size() - limit), history.size());
                
                // Create task with history
                // 创建包含历史的任务对象
                Task taskWithHistory = new Task(
                    task.id(),
                    task.contextId(),
                    task.kind(),
                    task.status(),
                    task.artifacts(),
                    limitedHistory,
                    task.metadata()
                );
                
                return createSuccessResponse(request.id(), taskWithHistory);
            }

            // 返回不带历史的任务
            return createSuccessResponse(request.id(), task);

        } catch (Exception e) {
            // 参数错误，返回错误响应
            return createErrorResponse(request.id(), ErrorCode.INVALID_REQUEST, "Invalid parameters");
        }
    }

    /**
     * Handle task cancel request
     * 处理任务取消请求，尝试将任务状态置为CANCELED
     */
    public JSONRPCResponse handleTaskCancel(JSONRPCRequest request) {
        try {
            TaskIDParams params = parseParams(request.params(), TaskIDParams.class);

            Task task = taskStore.get(params.id());
            if (task == null) {
                // 未找到任务，返回错误
                return createErrorResponse(request.id(), ErrorCode.TASK_NOT_FOUND, "Task not found");
            }

            // Check if task can be canceled
            // 检查任务是否可以取消（已完成、已取消、已失败的任务不可取消）
            if (task.status().state() == TaskState.COMPLETED || 
                task.status().state() == TaskState.CANCELED ||
                task.status().state() == TaskState.FAILED) {
                return createErrorResponse(request.id(), ErrorCode.TASK_NOT_CANCELABLE, "Task cannot be canceled");
            }

            // Create canceled status with timestamp
            // 创建取消状态
            TaskStatus canceledStatus = new TaskStatus(
                TaskState.CANCELED,
                null,  // No message
                Instant.now().toString()
            );

            // Update task status to canceled
            // 更新任务状态为已取消
            Task canceledTask = new Task(
                task.id(),
                task.contextId(),
                task.kind(),
                canceledStatus,
                task.artifacts(),
                task.history(),
                task.metadata()
            );
            
            taskStore.put(params.id(), canceledTask);

            // 返回已取消的任务
            return createSuccessResponse(request.id(), canceledTask);

        } catch (Exception e) {
            // 参数错误，返回错误响应
            return createErrorResponse(request.id(), ErrorCode.INVALID_REQUEST, "Invalid parameters");
        }
    }

    /**
     * Get agent card information
     * 获取Agent Card信息
     */
    public AgentCard getAgentCard() {
        return agentCard;
    }

    /**
     * Get task history
     * 获取指定任务的消息历史
     */
    public List<Message> getTaskHistory(String taskId) {
        return taskHistory.getOrDefault(taskId, List.of());
    }

    /**
     * Parse request parameters
     * 解析请求参数为指定类型
     */
    private <T> T parseParams(Object params, Class<T> clazz) throws Exception {
        return objectMapper.convertValue(params, clazz);
    }

    /**
     * Create success response
     * 创建成功响应对象
     */
    private JSONRPCResponse createSuccessResponse(Object id, Object result) {
        return new JSONRPCResponse(
            id,
            "2.0",
            result,
            null
        );
    }

    /**
     * Create error response
     * 创建错误响应对象
     */
    private JSONRPCResponse createErrorResponse(Object id, ErrorCode code, String message) {
        JSONRPCError error = new JSONRPCError(code.getValue(), message, null);
        return new JSONRPCResponse(
            id,
            "2.0",
            null,
            error
        );
    }
}
