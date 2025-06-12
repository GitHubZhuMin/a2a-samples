package com.google.a2a.client;

import com.google.a2a.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A2A protocol client implementation
 * A2A协议客户端实现类
 */
public class A2AClient {
    
    private final String baseUrl; // 服务器基础URL
    private final HttpClient httpClient; // HTTP客户端
    private final ObjectMapper objectMapper; // JSON对象映射器
    
    /**
     * Create a new A2A client
     * 创建一个新的A2A客户端
     * 
     * @param baseUrl the base URL of the A2A server
     *                A2A服务器的基础URL
     */
    public A2AClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Create a new A2A client with custom HTTP client
     * 使用自定义HTTP客户端创建A2A客户端
     * 
     * @param baseUrl the base URL of the A2A server
     *                A2A服务器的基础URL
     * @param httpClient custom HTTP client
     *                   自定义HTTP客户端
     */
    public A2AClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Send a task message to the agent
     * 向代理发送任务消息
     * 
     * @param params task send parameters
     *               任务发送参数
     * @return JSON-RPC response containing the task
     *         包含任务的JSON-RPC响应
     * @throws A2AClientException if the request fails
     *                           请求失败时抛出异常
     */
    public JSONRPCResponse sendTask(TaskSendParams params) throws A2AClientException {
        JSONRPCRequest request = new JSONRPCRequest(
            generateRequestId(),
            "2.0",
            "tasks/send",
            params
        );
        
        return doRequest(request);
    }
    
    /**
     * Get the status of a task
     * 获取任务状态
     * 
     * @param params task query parameters
     *               任务查询参数
     * @return JSON-RPC response containing the task
     *         包含任务的JSON-RPC响应
     * @throws A2AClientException if the request fails
     *                           请求失败时抛出异常
     */
    public JSONRPCResponse getTask(TaskQueryParams params) throws A2AClientException {
        JSONRPCRequest request = new JSONRPCRequest(
            generateRequestId(),
            "2.0",
            "tasks/get",
            params
        );
        
        return doRequest(request);
    }
    
    /**
     * Cancel a task
     * 取消任务
     * 
     * @param params task ID parameters
     *               任务ID参数
     * @return JSON-RPC response containing the task
     *         包含任务的JSON-RPC响应
     * @throws A2AClientException if the request fails
     *                           请求失败时抛出异常
     */
    public JSONRPCResponse cancelTask(TaskIDParams params) throws A2AClientException {
        JSONRPCRequest request = new JSONRPCRequest(
            generateRequestId(),
            "2.0",
            "tasks/cancel",
            params
        );
        
        return doRequest(request);
    }
    
    /**
     * Send a task with streaming response
     * 发送任务并接收流式响应
     * 
     * @param params task send parameters
     *               任务发送参数
     * @param listener event listener for streaming updates
     *                 用于流式更新的事件监听器
     * @return CompletableFuture that completes when streaming ends
     *         流结束时完成的CompletableFuture
     */
    public CompletableFuture<Void> sendTaskStreaming(TaskSendParams params, StreamingEventListener listener) {
        return CompletableFuture.runAsync(() -> {
            try {
                JSONRPCRequest request = new JSONRPCRequest(
                    generateRequestId(),
                    "2.0",
                    "tasks/send",
                    params
                );
                
                String requestBody = objectMapper.writeValueAsString(request);
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/a2a/stream"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    listener.onError(new A2AClientException("HTTP " + response.statusCode() + ": " + response.body()));
                    return;
                }
                
                // Parse streaming response
                // 解析流式响应
                String[] lines = response.body().split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    try {
                        SendTaskStreamingResponse streamingResponse = objectMapper.readValue(line, SendTaskStreamingResponse.class);
                        
                        if (streamingResponse.error() != null) {
                            A2AError error = streamingResponse.error();
                            Integer errorCode = error.code() != null ? error.code().getValue() : null;
                            listener.onError(new A2AClientException(
                                error.message(),
                                errorCode
                            ));
                            return;
                        }
                        
                        if (streamingResponse.result() != null) {
                            listener.onEvent(streamingResponse.result());
                        }
                        
                    } catch (Exception e) {
                        listener.onError(new A2AClientException("Failed to parse streaming response", e));
                        return;
                    }
                }
                
                listener.onComplete();
                
            } catch (Exception e) {
                listener.onError(new A2AClientException("Streaming request failed", e));
            }
        });
    }
    
    /**
     * Get agent card information
     * 获取Agent Card信息
     * 
     * @return the agent card
     *         返回Agent Card
     * @throws A2AClientException if the request fails
     *                           请求失败时抛出异常
     */
    public AgentCard getAgentCard() throws A2AClientException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/.well-known/agent-card"))
                .header("Accept", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new A2AClientException("HTTP " + response.statusCode() + ": " + response.body());
            }
            
            return objectMapper.readValue(response.body(), AgentCard.class);
            
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Failed to get agent card", e);
        }
    }
    
    /**
     * Perform HTTP request and handle response
     * 执行HTTP请求并处理响应
     */
    private JSONRPCResponse doRequest(JSONRPCRequest request) throws A2AClientException {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/a2a"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new A2AClientException("HTTP " + response.statusCode() + ": " + response.body());
            }
            
            // Parse the response
            // 解析响应内容
            JsonNode responseNode = objectMapper.readTree(response.body());
            
            // Extract basic fields
            // 提取基础字段
            Object id = responseNode.has("id") ? responseNode.get("id").asText() : null;
            String jsonrpc = responseNode.get("jsonrpc").asText();
            
            // Handle error
            // 处理错误
            JSONRPCError error = null;
            if (responseNode.has("error") && !responseNode.get("error").isNull()) {
                error = objectMapper.treeToValue(responseNode.get("error"), JSONRPCError.class);
            }
            
            // Handle result
            // 处理结果
            Object result = null;
            if (responseNode.has("result") && !responseNode.get("result").isNull()) {
                result = objectMapper.treeToValue(responseNode.get("result"), Task.class);
            }
            
            JSONRPCResponse jsonrpcResponse = new JSONRPCResponse(id, jsonrpc, result, error);
            
            // Check for A2A errors
            // 检查A2A错误
            if (error != null) {
                throw new A2AClientException(error.message(), error.code());
            }
            
            return jsonrpcResponse;
            
        } catch (IOException | InterruptedException e) {
            throw new A2AClientException("Request failed", e);
        }
    }
    
    /**
     * Generate a unique request ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
} 