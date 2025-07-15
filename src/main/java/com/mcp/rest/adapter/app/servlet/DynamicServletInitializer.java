package com.mcp.rest.adapter.app.servlet;

import com.mcp.rest.adapter.app.auth.AuthenticatorFactory;
import com.mcp.rest.adapter.app.auth.McpAuthenticator;
import com.mcp.rest.adapter.app.config.McpServerConfig;
import com.mcp.rest.adapter.app.ratelimit.RateLimiter;
import com.mcp.rest.adapter.app.ratelimit.TpsRateLimiter;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态Servlet初始化器
 * 负责注册主路由Servlet，并提供动态注册和移除transportProvider的能力
 */
@Component
public class DynamicServletInitializer implements ServletContextInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DynamicServletInitializer.class);

    // MCP
    public static final String MCP_ENDPOINT = "/mcp";

    // SSE端点
    public static final String SSE_ENDPOINT = "/sse";
    // 消息端点
    public static final String MESSAGE_ENDPOINT = "/message";
    
    // HTTP状态码
    public static final int SC_TOO_MANY_REQUESTS = 429; // HTTP 429 Too Many Requests

    // 存储服务实例
    public static final Map<String, ServerInstance> serverInstanceMap = new ConcurrentHashMap<>();
    
    // 存储服务配置
    public static final Map<String, McpServerConfig> serverConfigMap = new ConcurrentHashMap<>();
    
    // 鉴权器
    private static McpAuthenticator authenticator;
    
    // 限流器
    private static RateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        logger.info("DynamicServletInitializer初始化中...");
        // 初始化鉴权器和限流器
        authenticator = AuthenticatorFactory.createAuthenticator(serverConfigMap);
        rateLimiter = new TpsRateLimiter(serverConfigMap);
    }

    @Override
    public void onStartup(ServletContext ctx) {
        // 注册路由Servlet
        ServletRegistration.Dynamic mcpRouterServlet = ctx.addServlet("mcpRouterServlet", new McpRouterServlet());
        
        // 添加映射 - 处理所有SSE和消息请求
        mcpRouterServlet.addMapping(MCP_ENDPOINT + "/*");
        mcpRouterServlet.setLoadOnStartup(1);
        mcpRouterServlet.setAsyncSupported(true);
        
        logger.info("McpRouterServlet已注册，处理路径: {}", MCP_ENDPOINT);
    }
    
    /**
     * 注册新的传输提供者
     * @param serverId 服务ID
     * @param transportProvider 传输提供者
     * @param mcpServer MCP服务器
     * @param config MCP服务器配置
     */
    public static void registerTransportProvider(String serverId, HttpServletSseServerTransportProvider transportProvider, 
                                               McpSyncServer mcpServer, McpServerConfig config) {
        ServerInstance serverInstance = new ServerInstance(serverId, transportProvider, mcpServer);
        serverInstanceMap.put(serverId, serverInstance);
        serverConfigMap.put(serverId, config);
        
        // 更新鉴权器和限流器
        authenticator = AuthenticatorFactory.createAuthenticator(serverConfigMap);
        rateLimiter = new TpsRateLimiter(serverConfigMap);
        
        logger.info("已注册传输提供者, serverId: {}, 鉴权类型: {}, 限流启用: {}", 
                serverId, config.getAuthType(), config.isEnableRateLimiting());
    }
    
    /**
     * 移除传输提供者
     * @param serverId 服务ID
     * @return 如果成功移除返回true，否则返回false
     */
    public static boolean removeTransportProvider(String serverId) {
        ServerInstance removed = serverInstanceMap.remove(serverId);
        McpServerConfig removedConfig = serverConfigMap.remove(serverId);
        
        if (removed != null) {
            // 更新鉴权器和限流器
            authenticator = AuthenticatorFactory.createAuthenticator(serverConfigMap);
            rateLimiter = new TpsRateLimiter(serverConfigMap);
            
            logger.info("已移除传输提供者, serverId: {}", serverId);
            return true;
        }
        return false;
    }
    
    /**
     * 获取传输提供者
     * @param serverId 服务ID
     * @return 对应的传输提供者，如果不存在返回null
     */
    public static HttpServletSseServerTransportProvider getTransportProvider(String serverId) {
        ServerInstance serverInstance = serverInstanceMap.get(serverId);
        return serverInstance != null ? serverInstance.transportProvider : null;
    }
    
    /**
     * 获取所有服务ID
     * @return 所有已注册的服务ID集合
     */
    public static Set<String> getAllServerIds() {
        return serverInstanceMap.keySet();
    }
    
    /**
     * 路由Servlet - 负责将请求转发给正确的传输提供者
     */
    public static class McpRouterServlet extends HttpServlet {
        private static final Logger log = LoggerFactory.getLogger(McpRouterServlet.class);
        
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String path = request.getRequestURI();
            log.info("接收到请求: {}, 查询参数: {}", path, request.getParameterMap());
            
            String serverId = extractServerId(path, request);
            
            if (serverId == null) {
                log.warn("请求缺少serverId参数: {}", path);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing serverId parameter");
                return;
            }
            
            log.info("提取的serverId: {}", serverId);

            ServerInstance serverInstance = serverInstanceMap.get(serverId);
            if (serverInstance == null) {
                log.warn("找不到服务ID对应的传输提供者: {}", serverId);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Server with ID " + serverId + " not found");
                return;
            }
            
            // 生成客户端唯一标识
            String clientId = generateClientId(request);
            
            // 鉴权检查
            if (!authenticator.authenticate(request, serverId)) {
                log.warn("鉴权失败, serverId: {}, clientId: {}", serverId, clientId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
                return;
            }
            
            // 限流检查
            if (!rateLimiter.tryAcquire(serverId, clientId)) {
                log.warn("请求被限流, serverId: {}, clientId: {}", serverId, clientId);
                response.sendError(SC_TOO_MANY_REQUESTS, "Rate limit exceeded");
                return;
            }
            
            try {
                HttpServletSseServerTransportProvider provider = serverInstance.transportProvider;
                
                log.info("转发请求到传输提供者, serverId: {}, clientId: {}", serverId, clientId);
                // 将请求转发给对应的传输提供者
                provider.service(request, response);
            } finally {
                // 释放限流资源
                rateLimiter.release(serverId, clientId);
            }
        }
        
        /**
         * 从请求路径或参数中提取服务ID
         */
        private String extractServerId(String path, HttpServletRequest request) {
            // 首先尝试从路径中提取，格式：/mcp/serverId/(sse|message)
            String[] segments = path.split("/");
            log.debug("路径段: {}", String.join(", ", segments));
            
            if (segments.length >= 3) {
                log.debug("从路径中提取serverId: {}", segments[2]);
                return segments[2]; // 第2个段是serverId
            }
            
            // 如果路径中没有，则尝试从请求参数中获取
            String paramServerId = request.getParameter("serverId");
            log.debug("从参数中提取serverId: {}", paramServerId);
            return paramServerId;
        }
        
        /**
         * 生成客户端唯一标识
         */
        private String generateClientId(HttpServletRequest request) {
            // 尝试使用客户端IP和用户代理作为标识
            String clientIp = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            
            // 提取身份标识
            String identity = authenticator.extractIdentity(request);
            
            // 组合生成唯一标识
            return identity + "-" + clientIp + "-" + 
                   (userAgent != null ? userAgent.hashCode() : UUID.randomUUID().toString());
        }
    }

    /**
     * 服务器实例内部类
     */
    public static class ServerInstance {

        final String id;

        final HttpServletSseServerTransportProvider transportProvider;

        final McpSyncServer mcpServer;

        ServerInstance(String id, HttpServletSseServerTransportProvider transportProvider, McpSyncServer mcpServer) {
            this.id = id;
            this.transportProvider = transportProvider;
            this.mcpServer = mcpServer;
        }

        public String getId() {
            return id;
        }

        public HttpServletSseServerTransportProvider getTransportProvider() {
            return transportProvider;
        }

        public McpSyncServer getMcpServer() {
            return mcpServer;
        }
    }
}
