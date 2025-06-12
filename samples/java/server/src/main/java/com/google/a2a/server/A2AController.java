package com.google.a2a.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.a2a.model.A2AError;
import com.google.a2a.model.AgentCard;
import com.google.a2a.model.ErrorCode;
import com.google.a2a.model.JSONRPCError;
import com.google.a2a.model.JSONRPCRequest;
import com.google.a2a.model.JSONRPCResponse;
import com.google.a2a.model.SendTaskStreamingResponse;
import com.google.a2a.model.Task;
import com.google.a2a.model.TaskSendParams;
import com.google.a2a.model.TaskState;
import com.google.a2a.model.TaskStatus;
import com.google.a2a.model.TaskStatusUpdateEvent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * A2A REST controller for handling JSON-RPC requests
 * A2A REST控制器，用于处理JSON-RPC请求
 */
@RestController
public class A2AController {

    private final A2AServer server;
    private final ObjectMapper objectMapper;

    public A2AController(A2AServer server, ObjectMapper objectMapper) {
        this.server = server;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle JSON-RPC requests
     * 处理JSON-RPC请求的主入口，支持多种任务方法
     */
    @PostMapping(
            path = "/a2a",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<JSONRPCResponse> handleJsonRpcRequest(@RequestBody JSONRPCRequest request) {

        if (!"2.0".equals(request.jsonrpc())) {
            // 检查JSON-RPC协议版本
            JSONRPCError error = new JSONRPCError(
                    ErrorCode.INVALID_REQUEST.getValue(),
                    "Invalid JSON-RPC version",
                    null
            );
            JSONRPCResponse response = new JSONRPCResponse(
                    request.id(),
                    "2.0",
                    null,
                    error
            );
            // 返回错误响应，协议版本不符
            return ResponseEntity.badRequest().body(response);
        }

        // 根据method字段分发不同的任务处理逻辑
        JSONRPCResponse response = switch (request.method()) {
            case "tasks/send" -> server.handleTaskSend(request);
            case "tasks/get" -> server.handleTaskGet(request);
            case "tasks/cancel" -> server.handleTaskCancel(request);
            default -> {
                JSONRPCError error = new JSONRPCError(
                        ErrorCode.METHOD_NOT_FOUND.getValue(),
                        "Method not found",
                        null
                );
                yield new JSONRPCResponse(
                        request.id(),
                        "2.0",
                        null,
                        error
                );
            }
        };

        // 返回处理结果
        return ResponseEntity.ok(response);
    }

    /**
     * Handle streaming task requests (Server-Sent Events)
     * 处理流式任务请求（SSE服务端推送事件），用于实时推送任务状态
     */
    @PostMapping(
            value = "/a2a/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter handleStreamingTask(@RequestBody JSONRPCRequest request) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 异步处理任务，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                if (!"tasks/send".equals(request.method())) {
                    // 只支持tasks/send方法，其他方法报错
                    sendErrorEvent(emitter, request.id(), ErrorCode.METHOD_NOT_FOUND, "Method not found");
                    return;
                }

                TaskSendParams params = parseTaskSendParams(request.params());

                // Create initial status with timestamp
                // 创建初始任务状态，标记为WORKING
                TaskStatus initialStatus = new TaskStatus(
                    TaskState.WORKING,
                    null,  // No message initially
                    Instant.now().toString()
                );

                // Send initial status update
                // 发送初始状态事件到客户端
                TaskStatusUpdateEvent initialEvent = new TaskStatusUpdateEvent(
                        params.id(),
                        initialStatus,
                        false,  // final
                        null    // metadata
                );

                SendTaskStreamingResponse initialResponse = new SendTaskStreamingResponse(
                        request.id(),
                        "2.0",
                        initialEvent,
                        null
                );

                emitter.send(SseEmitter.event()
                        .name("task-update")
                        .data(objectMapper.writeValueAsString(initialResponse)));

                // Process task
                // 处理实际任务
                JSONRPCResponse taskResponse = server.handleTaskSend(request);

                if (taskResponse.error() != null) {
                    // 任务处理出错，推送错误事件
                    sendErrorEvent(emitter, request.id(), ErrorCode.INTERNAL_ERROR, taskResponse.error().message());
                    return;
                }

                // Send final status update
                // 发送最终状态事件，任务完成
                Task completedTask = (Task) taskResponse.result();
                TaskStatusUpdateEvent finalEvent = new TaskStatusUpdateEvent(
                        completedTask.id(),
                        completedTask.status(),
                        true,   // final
                        null    // metadata
                );

                SendTaskStreamingResponse finalResponse = new SendTaskStreamingResponse(
                        request.id(),
                        "2.0",
                        finalEvent,
                        null
                );

                emitter.send(SseEmitter.event()
                        .name("task-update")
                        .data(objectMapper.writeValueAsString(finalResponse)));

                emitter.complete();

            } catch (Exception e) {
                // 发生异常，推送错误事件
                sendErrorEvent(emitter, request.id(), ErrorCode.INTERNAL_ERROR, e.getMessage());
            }
        });

        // 返回SseEmitter对象，客户端可持续接收事件
        return emitter;
    }

    /**
     * Get agent card information
     * 获取Agent Card信息，描述AI代理能力
     */
    @GetMapping("/.well-known/agent-card")
    public ResponseEntity<AgentCard> getAgentCard() {
        return ResponseEntity.ok(server.getAgentCard());
    }

    /**
     * Parse TaskSendParams
     * 解析任务发送参数
     */
    private TaskSendParams parseTaskSendParams(Object params) throws Exception {
        return objectMapper.convertValue(params, TaskSendParams.class);
    }

    /**
     * Send error event
     * 发送错误事件到SSE客户端
     */
    private void sendErrorEvent(SseEmitter emitter, Object requestId, ErrorCode code, String message) {
        try {
            A2AError error = new A2AError(code, message, null);
            SendTaskStreamingResponse errorResponse = new SendTaskStreamingResponse(
                    requestId,
                    "2.0",
                    null,
                    error
            );

            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(errorResponse)));

            emitter.completeWithError(new RuntimeException(message));

        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
