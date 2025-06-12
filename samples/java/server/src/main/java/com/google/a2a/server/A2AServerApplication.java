package com.google.a2a.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A2A Server Spring Boot Application
 * A2A服务器Spring Boot应用程序入口
 */
// @SpringBootApplication 注解表示这是一个Spring Boot应用的主类，会自动进行组件扫描和自动配置
@SpringBootApplication
public class A2AServerApplication {
    
    public static void main(String[] args) {
        // main方法是Java应用的入口，SpringApplication.run会启动Spring Boot框架，加载所有配置和Bean，启动Web服务器
        SpringApplication.run(A2AServerApplication.class, args);
    }
} 