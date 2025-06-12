package com.google.a2a.client;

/**
 * Exception thrown by A2A client operations
 * A2A客户端操作抛出的异常
 */
public class A2AClientException extends Exception {
    
    private final Integer errorCode; // 错误码，可选
    
    /**
     * 构造仅包含消息的异常
     * @param message 异常消息
     */
    public A2AClientException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    /**
     * 构造包含消息和原因的异常
     * @param message 异常消息
     * @param cause 异常原因
     */
    public A2AClientException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }
    
    /**
     * 构造包含消息和错误码的异常
     * @param message 异常消息
     * @param errorCode 错误码
     */
    public A2AClientException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造包含消息、错误码和原因的异常
     * @param message 异常消息
     * @param errorCode 错误码
     * @param cause 异常原因
     */
    public A2AClientException(String message, Integer errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Get the A2A error code if available
     * 获取A2A错误码（如有）
     */
    public Integer getErrorCode() {
        return errorCode;
    }
} 