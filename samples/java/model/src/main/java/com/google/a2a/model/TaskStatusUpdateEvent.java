package com.google.a2a.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * TaskStatusUpdateEvent 表示任务状态更新事件
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskStatusUpdateEvent(
    /**
     * ID 是被更新任务的ID
     */
    @JsonProperty("id") String id,
    
    /**
     * Status 是任务的新状态
     */
    @JsonProperty("status") TaskStatus status,
    
    /**
     * Final 表示这是否是该任务的最终更新
     */
    @JsonProperty("final") Boolean finalUpdate,
    
    /**
     * Metadata 为该更新事件关联的可选元数据
     */
    @JsonProperty("metadata") Map<String, Object> metadata
) {
} 