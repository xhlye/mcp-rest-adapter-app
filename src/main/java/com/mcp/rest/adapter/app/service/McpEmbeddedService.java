/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.rest.adapter.app.request.CreateServerRequest;
import com.mcp.rest.adapter.app.response.ServerInfo;
import com.mcp.rest.adapter.app.servlet.DynamicServletInitializer;
import com.mcp.rest.adapter.app.util.McpConnectionUtil;
import com.mcp.rest.adapter.app.util.McpRestTools;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在同一个端点上托管多个MCP服务器的服务
 */
@Service
public class McpEmbeddedService {

    private static final Logger logger = LoggerFactory.getLogger(McpEmbeddedService.class);

    // 使用DynamicServletInitializer中定义的端点
    private static final String SSE_ENDPOINT = DynamicServletInitializer.SSE_ENDPOINT;
    private static final String MESSAGE_ENDPOINT = DynamicServletInitializer.MESSAGE_ENDPOINT;
    
    @Value("${server.port}")
    private int serverPort;
    
    @Value("${spring.application.name}")
    private String applicationName;

    // 只存储服务器信息，不再存储transportProvider
    private final Map<String, ServerInstance> serverInstances = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        logger.info("初始化McpEmbeddedService，端口：{}，应用名称：{}", serverPort, applicationName);
    }

    /**
     * 从Swagger JSON创建并启动MCP服务器
     */
    public ServerInfo createServerFromSwagger(String swaggerJson, String baseUrl, String serverName,
                                             String serverVersion) {
        // 生成唯一服务器ID
        String serverId = "server-" + System.currentTimeMillis();
        try {
            // 创建传输提供者
            HttpServletSseServerTransportProvider transportProvider = new HttpServletSseServerTransportProvider(
                    objectMapper, MESSAGE_ENDPOINT, SSE_ENDPOINT);

            // 创建请求对象
            CreateServerRequest request = new CreateServerRequest();
            request.setSwaggerJson(swaggerJson);
            request.setBaseUrl(baseUrl);
            request.setServerName(serverName);
            request.setServerVersion(serverVersion);

            // 使用McpRestTools创建MCP服务器规范
            McpServer.SyncSpecification serverSpec = McpRestTools.createSyncServerFromSwagger(transportProvider, request);

            // 构建MCP服务器
            McpSyncServer mcpServer = serverSpec.build();

            // 注册传输提供者到DynamicServletInitializer
            DynamicServletInitializer.registerTransportProvider(serverId, transportProvider);
            
            // 存储服务器实例（不再存储transportProvider）
            serverInstances.put(serverId, new ServerInstance(serverId, mcpServer));

            // 构建访问URL - 使用共享端点
            String serverUrl = "http://localhost:" + serverPort + SSE_ENDPOINT + "/" + serverId;

            logger.info("创建MCP服务器，ID '{}'，访问URL：{}", serverId, serverUrl);

            return new ServerInfo(serverId, serverUrl, serverName, serverVersion);
        }
        catch (Exception e) {
            logger.error("创建MCP服务器失败", e);
            throw new RuntimeException("创建MCP服务器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 测试调用MCP服务器
     * @param serverId 服务器ID
     * @param toolName 工具名称
     * @param params 调用参数
     * @return 调用结果的JSON字符串
     */
    public String testCall(String serverId, String toolName, Map<String, Object> params) {
        if (serverId == null || serverId.isEmpty()) {
            throw new IllegalArgumentException("服务器ID不能为空");
        }
        
        String baseUrl = "http://localhost:" + serverPort;
        
        logger.info("测试调用MCP服务器，服务器ID: {}，工具: {}，参数: {}", serverId, toolName, params);
        
        try {
            // 尝试使用多种可能的连接策略
            return McpConnectionUtil.tryAlternativeConnection(
                serverId, baseUrl, toolName, params);
        } catch (Exception e) {
            logger.error("测试调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("测试调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理SSE连接请求 - 不再需要，由DynamicServletInitializer处理
     * 保留此方法以兼容现有代码
     */
    public void handleSseRequest(String serverId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("转发SSE请求到路由Servlet，服务器ID: {}", serverId);
        
        // 检查服务器是否存在
        if (!serverInstances.containsKey(serverId)) {
            logger.warn("未找到ID为'{}'的服务器，可用服务器: {}", 
                       serverId, String.join(", ", serverInstances.keySet()));
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到指定的服务器");
            return;
        }
        
        // 转发到正确的端点
        String servletPath = SSE_ENDPOINT + "/" + serverId;
        if (!request.getRequestURI().endsWith(servletPath)) {
            logger.info("请求URI不匹配，重定向到正确的端点: {}", servletPath);
            response.sendRedirect(servletPath);
            return;
        }
        
        // 实际处理由DynamicServletInitializer的路由Servlet完成
        // 这里不需要额外处理
    }
    
    /**
     * 处理消息请求 - 不再需要，由DynamicServletInitializer处理
     * 保留此方法以兼容现有代码
     */
    public void handleMessageRequest(String serverId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("转发消息请求到路由Servlet，服务器ID: {}", serverId);
        
        // 检查服务器是否存在
        if (!serverInstances.containsKey(serverId)) {
            logger.warn("未找到ID为'{}'的服务器，可用服务器: {}", 
                       serverId, String.join(", ", serverInstances.keySet()));
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到指定的服务器");
            return;
        }
        
        // 转发到正确的端点
        String servletPath = MESSAGE_ENDPOINT + "/" + serverId;
        if (!request.getRequestURI().endsWith(servletPath) && request.getMethod().equals("GET")) {
            logger.info("请求URI不匹配，重定向到正确的端点: {}", servletPath);
            response.sendRedirect(servletPath);
            return;
        }
        
        // 实际处理由DynamicServletInitializer的路由Servlet完成
        // 这里不需要额外处理
    }

    /**
     * 处理基于会话ID的消息请求
     * 当客户端通过SSE连接获取会话ID后，会使用此方法发送消息
     * 现在由DynamicServletInitializer的路由Servlet处理
     */
    public void handleMessageRequestBySessionId(String sessionId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("转发基于会话ID的消息请求到路由Servlet，会话ID: {}", sessionId);
        
        // 这里不再需要手动处理，由DynamicServletInitializer的路由Servlet完成
        // 但为了兼容性，我们保留此方法
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "请使用新的API端点");
    }

    /**
     * 停止并移除指定的MCP服务器
     */
    public boolean stopServer(String serverId) {
        ServerInstance instance = serverInstances.remove(serverId);
        if (instance == null) {
            return false;
        }
        
        try {
            // 从DynamicServletInitializer中移除传输提供者
            DynamicServletInitializer.removeTransportProvider(serverId);
            
            // 关闭MCP服务器
            if (instance.mcpServer != null) {
                instance.mcpServer.close();
            }
            
            logger.info("停止MCP服务器，ID: {}", serverId);
            return true;
        } catch (Exception e) {
            logger.error("停止MCP服务器失败，ID: {}", serverId, e);
            return false;
        }
    }

    /**
     * 获取所有运行中的服务器信息
     */
    public Map<String, ServerInfo> getRunningServers() {
        Map<String, ServerInfo> result = new HashMap<>();
        
        for (Map.Entry<String, ServerInstance> entry : serverInstances.entrySet()) {
            String id = entry.getKey();
            ServerInstance instance = entry.getValue();
            
            // 构建访问URL
            String serverUrl = "http://localhost:" + serverPort + SSE_ENDPOINT + "/" + id;
            
            result.put(id, new ServerInfo(
                id,
                serverUrl,
                instance.mcpServer.getServerInfo().name(),
                instance.mcpServer.getServerInfo().version()
            ));
        }
        
        return result;
    }

    /**
     * 停止所有服务器
     */
    public void stopAllServers() {
        // 创建副本以避免并发修改异常
        for (String serverId : serverInstances.keySet().toArray(new String[0])) {
            stopServer(serverId);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("关闭McpEmbeddedService，停止所有MCP服务器...");
        stopAllServers();
    }

    /**
     * 服务器实例内部类 - 不再存储transportProvider
     */
    private static class ServerInstance {
        final String id;
        final McpSyncServer mcpServer;

        ServerInstance(String id, McpSyncServer mcpServer) {
            this.id = id;
            this.mcpServer = mcpServer;
        }
    }
} 