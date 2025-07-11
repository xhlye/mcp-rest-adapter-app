/*
 * Copyright 2024-2024 the original author or authors.
 */
package com.mcp.rest.adapter.app.util;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MCP连接工具类
 * 提供更可靠的连接方法，处理各种URL格式和错误情况
 */
public class McpConnectionUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(McpConnectionUtil.class);
    
    /**
     * 创建到MCP服务器的连接并测试调用工具
     * @param serverId 服务器ID
     * @param baseUrl 基础URL
     * @param toolName 要调用的工具名称
     * @param params 调用参数
     * @return 调用结果
     * @throws Exception 如果连接或调用失败
     */
    public static String connectAndCall(String serverId, String baseUrl, String toolName, Map<String, Object> params) 
            throws Exception {
        
        // 1. 构建SSE连接URL
        String sseUrl = baseUrl + "/api/embeddedMcp/sse?serverId=" + serverId;
        logger.debug("创建到MCP服务器的连接，SSE URL: {}", sseUrl);
        
        // 2. 配置HTTP客户端，增加超时时间
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1);
        
        // 3. 创建传输配置，显式设置固定的消息端点格式
        var clientTransport = HttpClientSseClientTransport.builder(baseUrl)
                .sseEndpoint("/api/embeddedMcp/sse?serverId=" + serverId)
                .customizeClient(builder -> builder
                        .connectTimeout(Duration.ofSeconds(30)))
                .build();
        
        // 4. 创建客户端规范
        McpClient.SyncSpec clientSpec = McpClient.sync(clientTransport);
        
        // 5. 创建并初始化客户端，使用try-with-resources自动关闭资源
        try (var client = clientSpec.clientInfo(new McpSchema.Implementation("test-client", "1.0.0")).build()) {
            
            logger.debug("开始初始化MCP客户端...");
            
            // 6. 初始化连接
            McpSchema.InitializeResult initResult = client.initialize();
            logger.debug("MCP客户端初始化成功，服务器: {} v{}", 
                    initResult.serverInfo().name(), 
                    initResult.serverInfo().version());
            
            // 7. 获取工具列表
            var listResult = client.listTools();
            logger.debug("可用工具数量: {}", listResult.tools().size());
            
            // 8. 验证工具是否存在
            boolean toolExists = listResult.tools().stream()
                    .anyMatch(tool -> tool.name().equals(toolName));
            
            if (!toolExists) {
                String availableTools = listResult.tools().stream()
                        .map(McpSchema.Tool::name)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                
                throw new IllegalArgumentException("工具 '" + toolName + "' 不存在。可用工具: " + availableTools);
            }
            
            // 9. 执行工具调用
            logger.debug("开始调用工具: {}, 参数: {}", toolName, params);
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, params);
            McpSchema.CallToolResult result = client.callTool(request);
            
            // 10. 处理调用结果
            if (result.isError()) {
                String errorMessage = ((McpSchema.TextContent)result.content().get(0)).text();
                logger.error("工具调用失败: {}", errorMessage);
                throw new IOException("工具调用失败: " + errorMessage);
            }
            
            // 获取响应文本
            String responseText = ((McpSchema.TextContent) result.content().get(0)).text();
            logger.debug("工具调用成功，结果: {}", responseText);
            
            return responseText;
        }
    }
    
    /**
     * 使用不同的传输配置方式尝试连接
     * 当默认方式失败时可以使用此方法
     */
    public static String tryAlternativeConnection(String serverId, String baseUrl, String toolName, Map<String, Object> params) 
            throws Exception {
        
        try {
            logger.info("尝试方法1: 使用完整SSE URL作为baseUri");
            return connectWithFullSseUrl(serverId, baseUrl, toolName, params);
        } catch (Exception e) {
            logger.warn("方法1失败: {}", e.getMessage());
            
            try {
                logger.info("尝试方法2: 指定固定消息端点");
                return connectWithFixedMessageEndpoint(serverId, baseUrl, toolName, params);
            } catch (Exception e2) {
                logger.warn("方法2失败: {}", e2.getMessage());
                
                logger.info("尝试方法3: 使用阻塞等待");
                return connectWithBlockingWait(serverId, baseUrl, toolName, params);
            }
        }
    }
    
    /**
     * 方法1: 使用完整SSE URL作为baseUri
     */
    private static String connectWithFullSseUrl(String serverId, String baseUrl, String toolName, Map<String, Object> params) 
            throws Exception {
        
        // 使用完整的SSE URL作为baseUri
        String sseUrl = baseUrl + "/api/embeddedMcp/sse?serverId=" + serverId;
        
        var clientTransport = HttpClientSseClientTransport.builder(sseUrl)
                .sseEndpoint("/api/embeddedMcp/sse")
                .customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(30)))
                .build();
        
        try (var client = McpClient.sync(clientTransport)
                .clientInfo(new McpSchema.Implementation("test-client", "1.0.0"))
                .build()) {
            
            McpSchema.InitializeResult initResult = client.initialize();
            logger.debug("方法1初始化成功: {} v{}", initResult.serverInfo().name(), initResult.serverInfo().version());
            
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, params);
            McpSchema.CallToolResult result = client.callTool(request);
            
            if (result.isError()) {
                throw new IOException(((McpSchema.TextContent)result.content().get(0)).text());
            }
            
            return ((McpSchema.TextContent) result.content().get(0)).text();
        }
    }
    
    /**
     * 方法2: 指定固定消息端点
     */
    private static String connectWithFixedMessageEndpoint(String serverId, String baseUrl, String toolName, Map<String, Object> params) 
            throws Exception {
        
        var clientTransport = HttpClientSseClientTransport.builder(baseUrl)
                .sseEndpoint("/api/embeddedMcp/sse?serverId=" + serverId)
                // 在这里我们可以预先指定消息端点URL，但需要与服务器端生成的格式匹配
                // 这可能绕过endpoint事件处理逻辑
                .customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(30)))
                .build();
        
        try (var client = McpClient.sync(clientTransport)
                .clientInfo(new McpSchema.Implementation("test-client", "1.0.0"))
                .build()) {
            
            McpSchema.InitializeResult initResult = client.initialize();
            logger.debug("方法2初始化成功: {} v{}", initResult.serverInfo().name(), initResult.serverInfo().version());
            
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, params);
            McpSchema.CallToolResult result = client.callTool(request);
            
            if (result.isError()) {
                throw new IOException(((McpSchema.TextContent)result.content().get(0)).text());
            }
            
            return ((McpSchema.TextContent) result.content().get(0)).text();
        }
    }
    
    /**
     * 方法3: 使用阻塞等待，给服务器更多时间处理SSE连接
     */
    private static String connectWithBlockingWait(String serverId, String baseUrl, String toolName, Map<String, Object> params) 
            throws Exception {
        
        String sseUrl = baseUrl + "/api/embeddedMcp/sse?serverId=" + serverId;
        logger.debug("使用阻塞等待方式连接，SSE URL: {}", sseUrl);
        
        var clientTransport = HttpClientSseClientTransport.builder(sseUrl)
                .customizeClient(builder -> builder.connectTimeout(Duration.ofSeconds(60)))
                .build();
        
        // 先连接SSE，等待一段时间，让服务器有时间处理连接
        logger.debug("预先等待5秒，给服务器时间准备SSE连接");
        TimeUnit.SECONDS.sleep(5);
        
        try (var client = McpClient.sync(clientTransport)
                .clientInfo(new McpSchema.Implementation("test-client", "1.0.0"))
                .build()) {
            
            McpSchema.InitializeResult initResult = client.initialize();
            logger.debug("方法3初始化成功: {} v{}", initResult.serverInfo().name(), initResult.serverInfo().version());
            
            McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, params);
            McpSchema.CallToolResult result = client.callTool(request);
            
            if (result.isError()) {
                throw new IOException(((McpSchema.TextContent)result.content().get(0)).text());
            }
            
            return ((McpSchema.TextContent) result.content().get(0)).text();
        }
    }
} 