package com.mcp.rest.adapter.app.servlet;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态Servlet初始化器
 * 负责注册主路由Servlet，并提供动态注册和移除transportProvider的能力
 */
@Component
public class DynamicServletInitializer implements ServletContextInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DynamicServletInitializer.class);
    
    // SSE端点
    public static final String SSE_ENDPOINT = "/api/embeddedMcp/sse";
    // 消息端点
    public static final String MESSAGE_ENDPOINT = "/api/embeddedMcp/message";

    // 存储服务ID到传输提供者的映射
    private static final Map<String, HttpServletSseServerTransportProvider> transportProviders = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("DynamicServletInitializer初始化中...");
    }

    @Override
    public void onStartup(ServletContext ctx) {
        // 注册路由Servlet
        ServletRegistration.Dynamic mcpRouterServlet = ctx.addServlet("mcpRouterServlet", new McpRouterServlet());
        
        // 添加映射 - 处理所有SSE和消息请求
        mcpRouterServlet.addMapping(SSE_ENDPOINT + "/*");
        mcpRouterServlet.addMapping(MESSAGE_ENDPOINT + "/*");
        mcpRouterServlet.setLoadOnStartup(1);
        mcpRouterServlet.setAsyncSupported(true);
        
        logger.info("McpRouterServlet已注册，处理路径: {}, {}", SSE_ENDPOINT, MESSAGE_ENDPOINT);
    }
    
    /**
     * 注册新的传输提供者
     * @param serverId 服务ID
     * @param transportProvider 传输提供者
     */
    public static void registerTransportProvider(String serverId, HttpServletSseServerTransportProvider transportProvider) {
        transportProviders.put(serverId, transportProvider);
        logger.info("已注册传输提供者, serverId: {}", serverId);
    }
    
    /**
     * 移除传输提供者
     * @param serverId 服务ID
     * @return 如果成功移除返回true，否则返回false
     */
    public static boolean removeTransportProvider(String serverId) {
        HttpServletSseServerTransportProvider removed = transportProviders.remove(serverId);
        if (removed != null) {
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
        return transportProviders.get(serverId);
    }
    
    /**
     * 获取所有服务ID
     * @return 所有已注册的服务ID集合
     */
    public static Set<String> getAllServerIds() {
        return transportProviders.keySet();
    }
    
    /**
     * 路由Servlet - 负责将请求转发给正确的传输提供者
     */
    public static class McpRouterServlet extends HttpServlet {
        private static final Logger servletLogger = LoggerFactory.getLogger(McpRouterServlet.class);
        
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String path = request.getRequestURI();
            servletLogger.info("接收到请求: {}, 查询参数: {}", path, request.getQueryString());
            
            String serverId = extractServerId(path, request);
            
            if (serverId == null) {
                servletLogger.warn("请求缺少serverId参数: {}", path);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing serverId parameter");
                return;
            }
            
            servletLogger.info("提取的serverId: {}", serverId);
            
            HttpServletSseServerTransportProvider provider = transportProviders.get(serverId);
            if (provider == null) {
                servletLogger.warn("找不到服务ID对应的传输提供者: {}", serverId);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Server with ID " + serverId + " not found");
                return;
            }
            
            servletLogger.info("转发请求到传输提供者, serverId: {}", serverId);
            // 将请求转发给对应的传输提供者
            provider.service(request, response);
        }
        
        /**
         * 从请求路径或参数中提取服务ID
         */
        private String extractServerId(String path, HttpServletRequest request) {
            // 首先尝试从路径中提取，格式：/api/embeddedMcp/(sse|message)/serverId
            String[] segments = path.split("/");
            servletLogger.debug("路径段: {}", String.join(", ", segments));
            
            if (segments.length >= 5) {
                servletLogger.debug("从路径中提取serverId: {}", segments[4]);
                return segments[4]; // 第5个段是serverId
            }
            
            // 如果路径中没有，则尝试从请求参数中获取
            String paramServerId = request.getParameter("serverId");
            servletLogger.debug("从参数中提取serverId: {}", paramServerId);
            return paramServerId;
        }
    }
}
