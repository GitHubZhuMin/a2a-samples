package com.google.a2a.client;

/**
 * Listener interface for streaming task events
 * 用于流式任务事件的监听器接口
 */
public interface StreamingEventListener {
    
    /**
     * Called when a streaming event is received
     * 收到流式事件时调用
     * 
     * @param event the event object (could be TaskStatusUpdateEvent or TaskArtifactUpdateEvent)
     *              事件对象（可能是TaskStatusUpdateEvent或TaskArtifactUpdateEvent）
     */
    void onEvent(Object event);
    
    /**
     * Called when an error occurs during streaming
     * 流式过程中发生错误时调用
     * 
     * @param exception the exception that occurred
     *                 发生的异常
     */
    void onError(Exception exception);
    
    /**
     * Called when the stream is completed
     * 流式处理完成时调用
     */
    void onComplete();
} 