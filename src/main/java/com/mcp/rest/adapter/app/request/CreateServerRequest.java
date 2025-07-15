/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.request;

import com.mcp.rest.adapter.app.config.McpServerConfig;

import java.util.Map;

/**
 * 创建MCP服务器的请求体
 */
public class CreateServerRequest {
    /**
     * 基础服务信息
     */
    private ServerInfo serverInfo = new ServerInfo();
    
    /**
     * API认证信息
     */
    private ApiAuth apiAuth = new ApiAuth();
    
    /**
     * MCP服务配置
     */
    private McpAuth mcpAuth = new McpAuth();
    
    /**
     * 性能配置
     */
    private PerformanceConfig performanceConfig = new PerformanceConfig();
    
    // Getters and setters for main class fields
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
    
    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo != null ? serverInfo : new ServerInfo();
    }
    
    public ApiAuth getApiAuth() {
        return apiAuth;
    }
    
    public void setApiAuth(ApiAuth apiAuth) {
        this.apiAuth = apiAuth != null ? apiAuth : new ApiAuth();
    }
    
    public McpAuth getMcpAuth() {
        return mcpAuth;
    }
    
    public void setMcpAuth(McpAuth mcpAuth) {
        this.mcpAuth = mcpAuth != null ? mcpAuth : new McpAuth();
    }
    
    public PerformanceConfig getPerformanceConfig() {
        return performanceConfig;
    }
    
    public void setPerformanceConfig(PerformanceConfig performanceConfig) {
        this.performanceConfig = performanceConfig != null ? performanceConfig : new PerformanceConfig();
    }
    
    /**
     * 构建MCP服务器配置
     * @return MCP服务器配置
     */
    public McpServerConfig buildMcpServerConfig() {
        McpServerConfig.Builder builder = McpServerConfig.builder();
        
        // 设置鉴权类型
        switch (mcpAuth.getMcpAuthType()) {
            case API_KEY:
                builder.apiKeyAuth();
                // 添加API Key
                for (Map.Entry<String, String> entry : mcpAuth.getMcpApiKeys().entrySet()) {
                    builder.addApiKey(entry.getKey(), entry.getValue());
                }
                break;
            case JWT_TOKEN:
                builder.jwtAuth(mcpAuth.getMcpJwtSecret());
                break;
            case CUSTOM:
                builder.customAuth(mcpAuth.getMcpCustomAuthClass());
                break;
            default:
                builder.noAuth();
        }
        
        // 设置限流配置
        builder.enableRateLimiting(performanceConfig.isEnableRateLimiting());
        if (performanceConfig.isEnableRateLimiting()) {
            builder.maxTps(performanceConfig.getMaxTps())
                  .maxConcurrentRequests(performanceConfig.getMaxConcurrentRequests())
                  .requestTimeout(performanceConfig.getRequestTimeoutMs());
        }
        
        return builder.build();
    }
} 