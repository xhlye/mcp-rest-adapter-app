/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest;

import io.modelcontextprotocol.app.rest.service.McpEmbeddedService;
import io.modelcontextprotocol.app.rest.service.McpRestApiService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * MCP REST API服务器应用程序入口
 */
@SpringBootApplication
public class McpRestApiServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpRestApiServerApplication.class);
    
    private static McpRestApiService mcpRestApiService;
    private static McpEmbeddedService mcpEmbeddedService;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(McpRestApiServerApplication.class, args);
        
        // 获取服务实例并保存引用，以便在JVM关闭时使用
        mcpRestApiService = context.getBean(McpRestApiService.class);
        mcpEmbeddedService = context.getBean(McpEmbeddedService.class);
        
        // 添加JVM关闭钩子，确保所有服务器实例被正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutting down, stopping all MCP servers...");
            if (mcpRestApiService != null) {
                mcpRestApiService.stopAllServers();
            }
            if (mcpEmbeddedService != null) {
                mcpEmbeddedService.stopAllServers();
            }
            logger.info("All MCP servers stopped");
        }));
        
        logger.info("MCP REST API Server started");
    }
    
    @Bean
    public McpRestApiService mcpRestApiService() {
        return new McpRestApiService();
    }
    
    @Bean
    public McpEmbeddedService mcpEmbeddedService() {
        return new McpEmbeddedService();
    }
    
    /**
     * 在应用关闭时停止所有服务器实例
     */
    @PreDestroy
    public void onShutdown() {
        logger.info("Spring context closing, stopping all MCP servers...");
        if (mcpRestApiService != null) {
            mcpRestApiService.stopAllServers();
        }
        if (mcpEmbeddedService != null) {
            mcpEmbeddedService.stopAllServers();
        }
    }
} 