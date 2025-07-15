package com.mcp.rest.adapter.app.auth;

import com.mcp.rest.adapter.app.config.McpServerConfig;
import jakarta.servlet.http.HttpServletRequest;

/**
 * MCP服务鉴权接口
 */
public interface McpAuthenticator {
    /**
     * 验证请求是否有权限访问
     * @param request HTTP请求
     * @param serverId 服务ID
     * @return 如果验证通过返回true，否则返回false
     */
    boolean authenticate(HttpServletRequest request, String serverId);
    
    /**
     * 从请求中提取身份标识（如API Key ID或用户名）
     * @param request HTTP请求
     * @return 身份标识
     */
    String extractIdentity(HttpServletRequest request);
} 