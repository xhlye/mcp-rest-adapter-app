/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.app.rest.response.ServerInfo;
import io.modelcontextprotocol.app.rest.util.McpRestTools;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.Servlet;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 在同一个端点上托管多个MCP服务器的服务
 */
@Service
public class McpEmbeddedService {

    private static final Logger logger = LoggerFactory.getLogger(McpEmbeddedService.class);

    // SSE端点
    private static final String SSE_ENDPOINT = "/api/embeddedMcp/sse";

    // 消息端点
    private static final String MESSAGE_ENDPOINT = "/api/embeddedMcp/message";
    
    @Value("${server.port}")
    private int serverPort;
    
    @Value("${spring.application.name}")
    private String applicationName;

    private final Map<String, ServerInstance> serverInstances = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private ApplicationContext applicationContext;

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

            // 使用McpRestTools创建MCP服务器规范
            McpServer.SyncSpecification serverSpec = McpRestTools.createSyncServerFromSwagger(transportProvider,
                    swaggerJson, baseUrl, serverName, serverVersion);

            // 构建MCP服务器
            McpSyncServer mcpServer = serverSpec.build();
            
            // 存储服务器实例
            serverInstances.put(serverId, new ServerInstance(serverId, transportProvider, mcpServer));

            // 构建访问URL - 现在所有服务共享同一端点，通过serverId区分
            String serverUrl = "http://localhost:" + serverPort + SSE_ENDPOINT + "?serverId=" + serverId;

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
            return io.modelcontextprotocol.app.rest.util.McpConnectionUtil.tryAlternativeConnection(
                serverId, baseUrl, toolName, params);
        } catch (Exception e) {
            logger.error("测试调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("测试调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理SSE连接请求
     */
    public void handleSseRequest(String serverId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("开始处理SSE请求，服务器ID: {}, 请求URL: {}, 查询参数: {}", 
                    serverId, request.getRequestURL(), request.getQueryString());
        
        ServerInstance instance = serverInstances.get(serverId);
        if (instance == null) {
            logger.warn("未找到ID为'{}'的服务器，可用服务器: {}", 
                       serverId, String.join(", ", serverInstances.keySet()));
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到指定的服务器");
            return;
        }

        logger.debug("找到服务器实例: {}, 传输提供者: {}", serverId, instance.transportProvider);
        
        try {
            // 委托给实际的传输提供者处理SSE请求
            Servlet servlet = (Servlet)instance.transportProvider;
            logger.debug("处理SSE请求前，服务器: {}, Servlet类: {}", serverId, servlet.getClass().getName());
            servlet.service(request, response);
            logger.info("处理SSE请求完成，服务器ID: {}", serverId);
        } catch (ServletException e) {
            logger.error("处理SSE请求时出错，服务器ID: {}", serverId, e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "处理SSE请求失败");
            }
        }
    }
    
    /**
     * 处理消息请求
     */
    public void handleMessageRequest(String serverId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("开始处理消息请求，服务器ID: {}, 请求URL: {}, 内容类型: {}, 数据长度: {}", 
                    serverId, request.getRequestURL(), request.getContentType(), request.getContentLength());
        
        ServerInstance instance = serverInstances.get(serverId);
        if (instance == null) {
            logger.warn("未找到ID为'{}'的服务器，可用服务器: {}", 
                       serverId, String.join(", ", serverInstances.keySet()));
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到指定的服务器");
            return;
        }
        
        logger.debug("找到服务器实例: {}, 传输提供者: {}", serverId, instance.transportProvider);
        
        try {
            // 委托给实际的传输提供者处理消息请求
            Servlet servlet = (Servlet)instance.transportProvider;
            logger.debug("处理消息请求前，服务器: {}, Servlet类: {}", serverId, servlet.getClass().getName());
            servlet.service(request, response);
            logger.info("处理消息请求完成，服务器ID: {}", serverId);
        } catch (ServletException e) {
            logger.error("处理消息请求时出错，服务器ID: {}, 错误: {}", serverId, e.getMessage(), e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "处理消息请求失败");
            }
        }
    }

    /**
     * 处理基于会话ID的消息请求
     * 当客户端通过SSE连接获取会话ID后，会使用此方法发送消息
     */
    public void handleMessageRequestBySessionId(String sessionId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug("开始处理基于会话ID的消息请求，会话ID: {}, 请求URL: {}, 内容类型: {}, 数据长度: {}", 
                    sessionId, request.getRequestURL(), request.getContentType(), request.getContentLength());
        
        // 在这里，我们直接将请求转发给相应的传输提供者
        // 由于会话ID已经在SSE连接中建立，传输提供者应该能够识别此会话
        for (ServerInstance instance : serverInstances.values()) {
            try {
                logger.debug("尝试通过传输提供者处理消息请求，会话ID: {}", sessionId);
                
                Servlet servlet = (Servlet)instance.transportProvider;
                logger.debug("处理消息请求前，会话ID: {}, Servlet类: {}", sessionId, servlet.getClass().getName());
                
                // 处理消息请求
                servlet.service(request, response);
                
                // 如果响应已提交，表示请求已被处理
                if (response.isCommitted()) {
                    logger.info("消息请求已成功处理，会话ID: {}", sessionId);
                    return;
                }
            } catch (ServletException e) {
                logger.error("尝试使用传输提供者处理消息请求时出错，会话ID: {}", sessionId, e);
                // 继续尝试下一个服务器实例
            }
        }
        
        // 如果所有实例都无法处理请求，则返回404错误
        if (!response.isCommitted()) {
            logger.warn("未找到能处理会话ID: {} 的服务器实例", sessionId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到能处理指定会话ID的服务器实例");
        }
    }

    /**
     * 停止并移除指定的MCP服务器
     */
    public boolean stopServer(String serverId) {
        ServerInstance instance = serverInstances.remove(serverId);

        if (instance == null) {
            logger.warn("未找到ID为'{}'的服务器", serverId);
            return false;
        }

        try {
            // 关闭MCP服务器
            instance.mcpServer.close();
            
            // 关闭传输提供者
            instance.transportProvider.closeGracefully();
            
            logger.info("停止MCP服务器，ID '{}'", serverId);
            return true;
        }
        catch (Exception e) {
            logger.error("停止MCP服务器失败，ID '{}'", serverId, e);
            return false;
        }
    }

    /**
     * 获取所有运行中的服务器信息
     */
    public Map<String, ServerInfo> getRunningServers() {
        Map<String, ServerInfo> result = new ConcurrentHashMap<>();

        serverInstances.forEach((id, instance) -> {
            String serverUrl = "http://localhost:" + serverPort + SSE_ENDPOINT + "?serverId=" + id;

            result.put(id, new ServerInfo(id, serverUrl, instance.mcpServer.getServerInfo().name(),
                    instance.mcpServer.getServerInfo().version()));
        });

        return result;
    }

    /**
     * 关闭所有运行中的服务器
     */
    public void stopAllServers() {
        serverInstances.keySet().forEach(this::stopServer);
    }

    @PreDestroy
    public void cleanup() {
        logger.info("关闭所有MCP服务器实例");
        stopAllServers();
    }
    
    /**
     * 服务器实例内部类
     */
    private static class ServerInstance {
        final String id;
        final HttpServletSseServerTransportProvider transportProvider;
        final McpSyncServer mcpServer;

        ServerInstance(String id, HttpServletSseServerTransportProvider transportProvider, McpSyncServer mcpServer) {
            this.id = id;
            this.transportProvider = transportProvider;
            this.mcpServer = mcpServer;
        }
    }
} 