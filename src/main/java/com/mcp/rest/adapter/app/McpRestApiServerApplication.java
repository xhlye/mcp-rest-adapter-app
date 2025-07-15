/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app;

import com.mcp.rest.adapter.app.service.DynamicMcpServerService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * MCP REST API服务器应用程序入口
 */
@SpringBootApplication
@ServletComponentScan(basePackages = "com.mcp.rest.adapter.app.servlet")
public class McpRestApiServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(McpRestApiServerApplication.class);
    
    private static DynamicMcpServerService dynamicMcpServerService;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(McpRestApiServerApplication.class, args);
        
        // 获取服务实例并保存引用，以便在JVM关闭时使用
        dynamicMcpServerService = context.getBean(DynamicMcpServerService.class);
        
        // 添加JVM关闭钩子，确保所有服务器实例被正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutting down, stopping all MCP servers...");
            if (dynamicMcpServerService != null) {
                dynamicMcpServerService.stopAllServers();
            }
            logger.info("All MCP servers stopped");
        }));
        
        logger.info("MCP REST API Server started");
    }

    @Bean
    public DynamicMcpServerService mcpEmbeddedService() {
        return new DynamicMcpServerService();
    }

    /**
     * 在应用关闭时停止所有服务器实例
     */
    @PreDestroy
    public void onShutdown() {
        logger.info("Spring context closing, stopping all MCP servers...");
        if (dynamicMcpServerService != null) {
            dynamicMcpServerService.stopAllServers();
        }
    }
} 