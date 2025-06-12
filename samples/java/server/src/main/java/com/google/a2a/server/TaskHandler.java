package com.google.a2a.server;

import com.google.a2a.model.Task;
import com.google.a2a.model.Message;

/**
 * TaskHandler is a functional interface for handling tasks
 * TaskHandler是用于处理任务的函数式接口
 */
@FunctionalInterface
public interface TaskHandler {
    /**
     * Handle a task
     * 处理一个任务
     * 
     * @param task the task to handle
     *             要处理的任务
     * @param message the message content
     *                消息内容
     * @return the processed task
     *         处理后的任务
     * @throws Exception exceptions during processing
     *                   处理过程中可能抛出的异常
     */
    Task handle(Task task, Message message) throws Exception;
    // 实现此方法以根据传入的任务和消息进行处理，并返回新的任务状态
} 