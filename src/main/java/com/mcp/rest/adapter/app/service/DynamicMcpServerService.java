/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.rest.adapter.app.adapter.McpRestApiAdapter;
import com.mcp.rest.adapter.app.config.McpServerConfig;
import com.mcp.rest.adapter.app.request.CreateServerRequest;
import com.mcp.rest.adapter.app.response.ServerInfo;
import com.mcp.rest.adapter.app.servlet.DynamicServletInitializer;
import com.mcp.rest.adapter.app.util.McpRestTools;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在同一个端点上托管多个MCP服务器的服务
 */
@Service
public class DynamicMcpServerService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMcpServerService.class);
    
    @Value("${server.port}")
    private int serverPort;
    
    @Value("${spring.application.name}")
    private String applicationName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        logger.info("初始化McpEmbeddedService，端口：{}，应用名称：{}", serverPort, applicationName);
    }

    /**
     * 从Swagger JSON创建并启动MCP服务器
     */
    public ServerInfo createServerFromSwagger(CreateServerRequest request, HttpServletRequest httpRequest) {
        // 生成唯一服务器ID
        String serverId = "server-" + System.currentTimeMillis();
        try {
            // 使用适配器获取工具列表
            List<McpSchema.Tool> tools = null;
            try (McpRestApiAdapter adapter = McpRestApiAdapter.builder()
                    .swaggerJson(request.getServerInfo().getSwaggerJson())
                    .baseUrl(request.getServerInfo().getBaseUrl())
                    .build()) {
                tools = adapter.generateTools();
            }catch (Exception e) {
                throw new RuntimeException("Failed to get MCP server tolls from Swagger", e);
            }
            String sseEndpoint = DynamicServletInitializer.MCP_ENDPOINT + "/" + serverId + DynamicServletInitializer.SSE_ENDPOINT;
            String messageEndpoint = DynamicServletInitializer.MCP_ENDPOINT + "/" + serverId + DynamicServletInitializer.MESSAGE_ENDPOINT;
                    // 创建传输提供者
            HttpServletSseServerTransportProvider transportProvider = new HttpServletSseServerTransportProvider(
                    objectMapper, messageEndpoint, sseEndpoint);
            
            // 使用McpRestTools创建MCP服务器规范
            McpServer.SyncSpecification serverSpec = McpRestTools.createSyncServerFromSwagger(transportProvider, request);
            // 构建MCP服务器
            McpSyncServer mcpServer = serverSpec.build();
            // 构建MCP服务器配置
            McpServerConfig mcpServerConfig = request.buildMcpServerConfig();
            // 注册传输提供者到DynamicServletInitializer
            DynamicServletInitializer.registerTransportProvider(serverId, transportProvider, mcpServer, mcpServerConfig);
            // 构建访问URL - 使用共享端点
            String path = DynamicServletInitializer.MCP_ENDPOINT + "/" + serverId + DynamicServletInitializer.SSE_ENDPOINT;
            String serverUrl = McpRestTools.getServerUrl(httpRequest, serverPort, path);
            return new ServerInfo(serverId,
                    serverUrl,
                    request.getServerInfo().getServerName(),
                    request.getServerInfo().getServerVersion(),
                    tools);
        }
        catch (Exception e) {
            logger.error("创建MCP服务器失败", e);
            throw new RuntimeException("创建MCP服务器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止并移除指定的MCP服务器
     */
    public boolean stopServer(String serverId) {
        try {
            DynamicServletInitializer.ServerInstance serverInstance = DynamicServletInitializer.serverInstanceMap.get(serverId);
            // 关闭MCP服务器
            if (serverInstance.getMcpServer() != null) {
                serverInstance.getMcpServer().close();
            }
            // 从DynamicServletInitializer中移除传输提供者
            DynamicServletInitializer.removeTransportProvider(serverId);
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
    public Map<String, ServerInfo> getRunningServers(HttpServletRequest httpRequest) {
        Map<String, ServerInfo> result = new HashMap<>();
        for (Map.Entry<String, DynamicServletInitializer.ServerInstance> entry : DynamicServletInitializer.serverInstanceMap.entrySet()) {
            String id = entry.getKey();
            DynamicServletInitializer.ServerInstance instance = entry.getValue();
            // 构建访问URL - 使用动态主机名
            String path = DynamicServletInitializer.MCP_ENDPOINT + "/" + id + DynamicServletInitializer.SSE_ENDPOINT;
            String serverUrl = McpRestTools.getServerUrl(httpRequest, serverPort, path);
            result.put(id, new ServerInfo(
                id,
                serverUrl,
                instance.getMcpServer().getServerInfo().name(),
                instance.getMcpServer().getServerInfo().version()
            ));
        }
        return result;
    }

    /**
     * 停止所有服务器
     */
    public void stopAllServers() {
        // 创建副本以避免并发修改异常
        for (String serverId : DynamicServletInitializer.serverInstanceMap.keySet().toArray(new String[0])) {
            stopServer(serverId);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("关闭McpEmbeddedService，停止所有MCP服务器...");
        stopAllServers();
    }

} 