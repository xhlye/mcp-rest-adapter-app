package com.mcp.rest.adapter.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.rest.adapter.app.adapter.McpRestApiAdapter;
import com.mcp.rest.adapter.app.response.ServerInfo;
import com.mcp.rest.adapter.app.util.McpRestTools;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.mcp.rest.adapter.app.request.CreateServerRequest;

/**
 * 延迟启动MCP服务的服务层
 * 支持先创建服务配置，后续按需启动服务实例
 */
@Service
public class McpLazyService {

    private static final Logger logger = LoggerFactory.getLogger(McpLazyService.class);

    // 端口号生成器
    private final AtomicInteger portCounter = new AtomicInteger(8080);

    // SSE端点
    private static final String SSE_ENDPOINT = "/mcp/sse";

    // 消息端点
    private static final String MESSAGE_ENDPOINT = "/mcp/message";

    // 存储所有服务配置（包括未启动的）
    private final Map<String, ServerConfig> serverConfigs = new ConcurrentHashMap<>();

    // 存储运行中的服务实例
    private final Map<String, ServerInstance> runningServers = new ConcurrentHashMap<>();

    // 对象序列化/反序列化器
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建MCP服务配置（不启动服务）
     */
    public ServerInfo createServerConfig(String swaggerJson, String baseUrl, String serverName, String serverVersion) {
        // 生成唯一服务器ID
        String serverId = "server-" + System.currentTimeMillis();

        try {
            // 解析工具列表
            List<McpSchema.Tool> tools;
            try (McpRestApiAdapter adapter = McpRestApiAdapter.builder()
                    .swaggerJson(swaggerJson)
                    .baseUrl(baseUrl)
                    .build()) {
                tools = adapter.generateTools();
            }

            // 创建服务器配置
            ServerConfig config = new ServerConfig(
                serverId,
                swaggerJson,
                baseUrl,
                serverName,
                serverVersion,
                tools
            );

            // 存储配置
            serverConfigs.put(serverId, config);

            // 返回服务器信息（此时未启动，URL为null）
            return new ServerInfo(serverId, null, serverName, serverVersion, tools);
        } catch (Exception e) {
            logger.error("创建服务配置失败", e);
            throw new RuntimeException("创建服务配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 启动指定的MCP服务
     */
    public ServerInfo startServer(String serverId) {
        // 检查服务是否已经在运行
        if (runningServers.containsKey(serverId)) {
            return getServerInfo(serverId);
        }

        // 获取服务配置
        ServerConfig config = serverConfigs.get(serverId);
        if (config == null) {
            throw new RuntimeException("未找到服务配置: " + serverId);
        }

        try {
            // 分配可用的端口号
            int port;
            do {
                port = portCounter.getAndIncrement();
            } while (!isPortAvailable(port));

            String contextPath = "";

            // 创建传输提供者
            HttpServletSseServerTransportProvider transportProvider = new HttpServletSseServerTransportProvider(
                objectMapper, MESSAGE_ENDPOINT, SSE_ENDPOINT);

            // 创建请求对象
            CreateServerRequest request = new CreateServerRequest();
            request.setSwaggerJson(config.swaggerJson);
            request.setBaseUrl(config.baseUrl);
            request.setServerName(config.serverName);
            request.setServerVersion(config.serverVersion);

            // 创建MCP服务器规范
            McpServer.SyncSpecification serverSpec = McpRestTools.createSyncServerFromSwagger(
                transportProvider,
                request
            );

            // 构建MCP服务器
            McpSyncServer mcpServer = serverSpec.build();

            // 创建并启动Tomcat服务器
            Tomcat tomcat = createTomcatServer(contextPath, port, transportProvider);
            tomcat.start();

            // 存储运行实例
            ServerInstance instance = new ServerInstance(config, tomcat, mcpServer);
            runningServers.put(serverId, instance);

            // 构建访问URL
            String serverUrl = "http://localhost:" + port + contextPath + SSE_ENDPOINT;

            logger.info("启动MCP服务成功，ID: {}, URL: {}", serverId, serverUrl);

            return new ServerInfo(serverId, serverUrl, config.serverName, config.serverVersion, config.tools);
        } catch (Exception e) {
            logger.error("启动服务失败", e);
            throw new RuntimeException("启动服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取所有服务配置（包括未启动和已启动的）
     */
    public Map<String, ServerInfo> getAllServers() {
        Map<String, ServerInfo> result = new ConcurrentHashMap<>();

        serverConfigs.forEach((id, config) -> {
            ServerInstance instance = runningServers.get(id);
            String serverUrl = null;

            if (instance != null) {
                int port = instance.tomcat.getConnector().getPort();
                serverUrl = "http://localhost:" + port + SSE_ENDPOINT;
            }

            result.put(id, new ServerInfo(id, serverUrl, config.serverName, config.serverVersion, config.tools));
        });

        return result;
    }

    /**
     * 获取所有运行中的服务
     */
    public Map<String, ServerInfo> getRunningServers() {
        Map<String, ServerInfo> result = new ConcurrentHashMap<>();

        runningServers.forEach((id, instance) -> {
            int port = instance.tomcat.getConnector().getPort();
            String serverUrl = "http://localhost:" + port + SSE_ENDPOINT;

            result.put(id, new ServerInfo(
                id,
                serverUrl,
                instance.config.serverName,
                instance.config.serverVersion,
                instance.config.tools
            ));
        });

        return result;
    }

    /**
     * 获取指定服务信息
     */
    public ServerInfo getServerInfo(String serverId) {
        ServerConfig config = serverConfigs.get(serverId);
        if (config == null) {
            return null;
        }

        ServerInstance instance = runningServers.get(serverId);
        String serverUrl = null;

        if (instance != null) {
            int port = instance.tomcat.getConnector().getPort();
            serverUrl = "http://localhost:" + port + SSE_ENDPOINT;
        }

        return new ServerInfo(serverId, serverUrl, config.serverName, config.serverVersion, config.tools);
    }

    /**
     * 停止服务
     */
    public boolean stopServer(String serverId) {
        ServerInstance instance = runningServers.remove(serverId);

        if (instance == null) {
            return false;
        }

        try {
            instance.mcpServer.close();
            instance.tomcat.stop();
            instance.tomcat.destroy();
            logger.info("停止MCP服务成功，ID: {}", serverId);
            return true;
        } catch (Exception e) {
            logger.error("停止服务失败", e);
            return false;
        }
    }

    /**
     * 停止所有运行中的服务
     */
    public void stopAllServers() {
        runningServers.keySet().forEach(this::stopServer);
    }

    /**
     * 移除指定的服务配置
     * 如果服务正在运行，会先停止服务
     */
    public boolean removeServer(String serverId) {
        // 如果服务正在运行，先停止服务
        if (runningServers.containsKey(serverId)) {
            stopServer(serverId);
        }
        
        // 移除服务配置
        ServerConfig removed = serverConfigs.remove(serverId);
        
        if (removed != null) {
            logger.info("移除服务配置成功，ID: {}", serverId);
            return true;
        } else {
            logger.warn("未找到服务配置，ID: {}", serverId);
            return false;
        }
    }
    
    /**
     * 移除所有服务配置
     * 如果有服务正在运行，会先停止所有运行中的服务
     */
    public void removeAllServers() {
        // 先停止所有运行中的服务
        stopAllServers();
        
        // 清空所有服务配置
        serverConfigs.clear();
        logger.info("已移除所有服务配置");
    }

    /**
     * 创建Tomcat服务器
     */
    private Tomcat createTomcatServer(String contextPath, int port, HttpServletSseServerTransportProvider servlet) {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);

        String baseDir = System.getProperty("java.io.tmpdir");
        tomcat.setBaseDir(baseDir);

        Context context = tomcat.addContext(contextPath, baseDir);

        org.apache.catalina.Wrapper wrapper = context.createWrapper();
        wrapper.setName("mcpServlet");
        wrapper.setServlet(servlet);
        wrapper.setLoadOnStartup(1);
        wrapper.setAsyncSupported(true);
        context.addChild(wrapper);
        context.addServletMappingDecoded("/*", "mcpServlet");

        var connector = tomcat.getConnector();
        connector.setAsyncTimeout(3000);

        return tomcat;
    }

    /**
     * 检查端口是否可用
     * @param port 要检查的端口号
     * @return 如果端口可用返回true，否则返回false
     */
    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.close();
            return true;
        } catch (IOException e) {
            logger.debug("端口 {} 已被占用", port);
            return false;
        }
    }

    /**
     * 服务器配置内部类
     */
    private static class ServerConfig {
        final String serverId;
        final String swaggerJson;
        final String baseUrl;
        final String serverName;
        final String serverVersion;
        final List<McpSchema.Tool> tools;

        ServerConfig(String serverId, String swaggerJson, String baseUrl, String serverName, String serverVersion, List<McpSchema.Tool> tools) {
            this.serverId = serverId;
            this.swaggerJson = swaggerJson;
            this.baseUrl = baseUrl;
            this.serverName = serverName;
            this.serverVersion = serverVersion;
            this.tools = tools;
        }
    }

    /**
     * 服务器实例内部类
     */
    private static class ServerInstance {
        final ServerConfig config;
        final Tomcat tomcat;
        final McpSyncServer mcpServer;

        ServerInstance(ServerConfig config, Tomcat tomcat, McpSyncServer mcpServer) {
            this.config = config;
            this.tomcat = tomcat;
            this.mcpServer = mcpServer;
        }
    }
} 