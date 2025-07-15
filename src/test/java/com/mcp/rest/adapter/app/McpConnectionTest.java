/*
 * Copyright 2024-2024 the original author or authors.
 */
package com.mcp.rest.adapter.app;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * 测试MCP连接问题
 */
public class McpConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(McpConnectionTest.class);

    @Test
    public void testConnectionProblems() {
        // 使用服务器中创建的服务器ID
        String serverId = "server-1752226101706";
        String baseUrl = "http://localhost:8081";
        
        // SSE连接URL - 使用服务器ID参数
        //String sseUrl = baseUrl + "/sse/" + serverId;
        String sseUrl = baseUrl + "/mcp/sse";

        logger.info("尝试连接MCP服务器，SSE URL: {}", sseUrl);
        
        // 配置HTTP客户端，增加超时时间
        var httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30));
        
        // 构建客户端传输 - 使用默认方式，不指定sseEndpoint，完全依赖于服务器返回的endpoint
        var transport = HttpClientSseClientTransport.builder(sseUrl)
                .customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(30)))
                .build();
        
        // 创建客户端规范
        McpClient.SyncSpec clientBuilder = McpClient.sync(transport);
        
        // 创建并初始化客户端
        try (var client = clientBuilder.clientInfo(new McpSchema.Implementation("test-client", "1.0.0")).build()) {
            try {
                McpSchema.InitializeResult initResult = client.initialize();
                logger.info("客户端初始化成功, 连接到: {} v{}", 
                    initResult.serverInfo().name(), 
                    initResult.serverInfo().version());
                
                // 获取工具列表
                var listResult = client.listTools();
                logger.info("工具列表: {}", listResult.tools());
                
                // 测试调用简单工具
                var callResult = client.callTool(new McpSchema.CallToolRequest("testApi", Map.of("message", "Hello")));
                logger.info("调用结果: {}", callResult);
                
            } catch (Exception e) {
                logger.error("连接MCP服务器失败: {}", e.getMessage(), e);
                throw e;
            }
        }
    }
    
    @Test
    public void testDirectInitialize() {
        // 使用服务器中创建的服务器ID
        String serverId = "server-1747140731248";
        String baseUrl = "http://localhost:8080";
        
        // SSE连接URL - 使用服务器ID参数
        String sseUrl = baseUrl + "/sse/" + serverId;
        
        logger.info("尝试连接MCP服务器，SSE URL: {}", sseUrl);
        
        try {
            // 直接创建一个包含消息端点的客户端传输
            var transport = HttpClientSseClientTransport.builder(baseUrl)
                    .sseEndpoint("/api/embeddedMcp/sse?serverId=" + serverId)
                    .customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(30)))
                    .build();
            
            // 创建并初始化客户端
            try (var client = McpClient.sync(transport)
                    .clientInfo(new McpSchema.Implementation("test-client", "1.0.0"))
                    .build()) {
                
                McpSchema.InitializeResult initResult = client.initialize();
                logger.info("客户端初始化成功, 连接到: {} v{}", 
                    initResult.serverInfo().name(), 
                    initResult.serverInfo().version());
            }
        } catch (Exception e) {
            logger.error("连接MCP服务器失败: {}", e.getMessage(), e);
        }
    }
} 