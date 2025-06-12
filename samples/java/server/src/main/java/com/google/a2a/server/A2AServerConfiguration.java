package com.google.a2a.server;

import com.google.a2a.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A2A server configuration class - AI Translation Bot
 * A2A服务器配置类 - AI翻译机器人
 */
@Configuration
public class A2AServerConfiguration {

    /**
     * Configure A2AServer bean
     * 配置A2AServer的Bean
     */
    @Bean
    public A2AServer a2aServer(ObjectMapper objectMapper, ChatModel chatModel) {
        // Create translation agent card
        // 创建翻译Agent Card
        AgentCard agentCard = createTranslationAgentCard();

        // Create translation task handler
        // 创建翻译任务处理器
        TaskHandler taskHandler = createTranslationTaskHandler(chatModel);

        // 返回A2AServer实例，包含Agent Card、任务处理器和对象映射器
        return new A2AServer(agentCard, taskHandler, objectMapper);
    }

    /**
     * Create translation agent card
     * 创建翻译Agent Card，描述AI代理的能力、认证方式、技能等
     */
    private AgentCard createTranslationAgentCard() {
        AgentProvider provider = new AgentProvider(
            "Google",
            "https://google.com"
        );

        AgentCapabilities capabilities = new AgentCapabilities(
            true,  // streaming
            true,  // pushNotifications
            true   // stateTransitionHistory
        );

        AgentAuthentication authentication = new AgentAuthentication(
            List.of("bearer"),
            null
        );

        AgentSkill skill = new AgentSkill(
            "ai-translator",
            "AI Translation Service",
            "Professional AI translator supporting multiple languages. Can translate text between various language pairs including English, Chinese, Japanese, French, Spanish, German, and more.",
            // 专业AI翻译，支持多语言，可以在多种语言对之间翻译，包括英语、中文、日语、法语、西班牙语、德语等
            List.of("translation", "language", "ai", "multilingual"),
            List.of(
                "Example: Translate 'Hello World' to Chinese",
                "Example: 请把这句话翻译成英文: '你好'",
                "Example: Translate from French to Spanish: 'Bonjour le monde'"
            ),
            List.of("text"),
            List.of("text")
        );

        return new AgentCard(
            "AI Translation Bot",
            "Professional AI translation service powered by advanced language models. Supports translation between multiple languages with high accuracy and context awareness.",
            // 专业AI翻译服务，基于先进的语言模型，支持多语言高精度、上下文感知翻译
            "http://localhost:8080/a2a",
            provider,
            "1.0.0",
            "http://localhost:8080/docs",
            capabilities,
            authentication,
            List.of("text"),
            List.of("text"),
            List.of(skill)
        );
    }

    /**
     * Create translation task handler using ChatClient
     * 使用ChatClient创建翻译任务处理器
     */
    private TaskHandler createTranslationTaskHandler(ChatModel chatModel) {
        ChatClient chatClient = ChatClient.create(chatModel);

        return (task, message) -> {
            try {
                // Extract text content from message parts
                // 从消息内容中提取需要翻译的文本
                String textToTranslate = extractTextFromMessage(message);

                if (textToTranslate == null || textToTranslate.trim().isEmpty()) {
                    // 如果没有文本内容，返回错误任务
                    return createErrorTask(task, "No text content found in the message");
                }

                // Create translation prompt
                // 构造翻译提示词
                String translationPrompt = createTranslationPrompt(textToTranslate);

                // Call ChatClient for translation
                // 调用ChatClient进行翻译
                String translatedText = chatClient
                    .prompt(translationPrompt)
                    .call()
                    .content();

                // Create response message with translation
                // 创建包含翻译结果的响应消息
                TextPart responsePart = new TextPart(translatedText, null);
                Message responseMessage = new Message(
                    UUID.randomUUID().toString(),
                    "message",
                    "assistant",
                    List.of(responsePart),
                    message.contextId(),
                    task.id(),
                    List.of(message.messageId()),
                    null
                );

                // Create completed status
                // 创建已完成状态
                TaskStatus completedStatus = new TaskStatus(
                    TaskState.COMPLETED,
                    null,  // No status message
                    Instant.now().toString()
                );

                // Add response to history
                // 将响应消息加入历史记录
                List<Message> updatedHistory = task.history() != null ?
                    List.of(task.history().toArray(new Message[0])) :
                    List.of();
                updatedHistory = List.of(
                    java.util.stream.Stream.concat(
                        updatedHistory.stream(),
                        java.util.stream.Stream.of(message, responseMessage)
                    ).toArray(Message[]::new)
                );

                // 返回新的Task对象，包含翻译结果和历史
                return new Task(
                    task.id(),
                    task.contextId(),
                    task.kind(),
                    completedStatus,
                    task.artifacts(),
                    updatedHistory,
                    task.metadata()
                );

            } catch (Exception e) {
                // 发生异常时，返回错误任务
                return createErrorTask(task, "Translation failed: " + e.getMessage());
            }
        };
    }

    /**
     * Extract text content from message parts
     * 从消息的各个部分提取文本内容
     */
    private String extractTextFromMessage(Message message) {
        if (message.parts() == null || message.parts().isEmpty()) {
            return null;
        }

        StringBuilder textBuilder = new StringBuilder();
        for (Part part : message.parts()) {
            if (part instanceof TextPart textPart) {
                if (textBuilder.length() > 0) {
                    textBuilder.append("\n");
                }
                textBuilder.append(textPart.text());
            }
        }

        return textBuilder.toString();
    }

    /**
     * Create translation prompt for ChatClient
     * 为ChatClient构造翻译提示词
     */
    private String createTranslationPrompt(String text) {
        return String.format("""
            You are a professional translator. Please translate the following text to the most appropriate target language.
            
            Instructions:
            1. If the text is in Chinese, translate to English
            2. If the text is in English, translate to Chinese
            3. If the text is in other languages, translate to English
            4. Maintain the original meaning and context
            5. Provide natural, fluent translations
            6. Only return the translated text, no explanations
            
            Text to translate: %s
            """, text);
        // 你是一名专业翻译，请将下列文本翻译为最合适的目标语言。
        // 1. 如果是中文，翻译成英文；2. 如果是英文，翻译成中文；3. 其他语言翻译成英文；4. 保持原意和上下文；5. 只返回翻译结果，不要解释。
    }

    /**
     * Create error task for translation failures
     * 创建翻译失败时的错误任务
     */
    private Task createErrorTask(Task originalTask, String errorMessage) {
        TaskStatus errorStatus = new TaskStatus(
            TaskState.FAILED,
            null,  // No status message
            Instant.now().toString()
        );

        return new Task(
            originalTask.id(),
            originalTask.contextId(),
            originalTask.kind(),
            errorStatus,
            originalTask.artifacts(),
            originalTask.history(),
            originalTask.metadata()
        );
    }
}
