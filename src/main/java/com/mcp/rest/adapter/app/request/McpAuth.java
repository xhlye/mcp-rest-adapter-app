/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.request;

import com.mcp.rest.adapter.app.config.McpServerConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP服务鉴权配置
 */
public class McpAuth {
    /**
     * MCP服务鉴权类型
     */
    private McpServerConfig.AuthType mcpAuthType = McpServerConfig.AuthType.NONE;
    
    /**
     * MCP服务API Key映射 (keyId -> secretKey)
     */
    private Map<String, String> mcpApiKeys = new HashMap<>();
    
    /**
     * MCP服务JWT密钥
     */
    private String mcpJwtSecret;
    
    /**
     * MCP服务自定义鉴权实现类名
     */
    private String mcpCustomAuthClass;
    
    public McpServerConfig.AuthType getMcpAuthType() {
        return mcpAuthType;
    }
    
    public void setMcpAuthType(McpServerConfig.AuthType mcpAuthType) {
        this.mcpAuthType = mcpAuthType;
    }
    
    public Map<String, String> getMcpApiKeys() {
        return mcpApiKeys;
    }
    
    public void setMcpApiKeys(Map<String, String> mcpApiKeys) {
        this.mcpApiKeys = mcpApiKeys != null ? mcpApiKeys : new HashMap<>();
    }
    
    public void addMcpApiKey(String keyId, String secretKey) {
        this.mcpApiKeys.put(keyId, secretKey);
    }
    
    public String getMcpJwtSecret() {
        return mcpJwtSecret;
    }
    
    public void setMcpJwtSecret(String mcpJwtSecret) {
        this.mcpJwtSecret = mcpJwtSecret;
    }
    
    public String getMcpCustomAuthClass() {
        return mcpCustomAuthClass;
    }
    
    public void setMcpCustomAuthClass(String mcpCustomAuthClass) {
        this.mcpCustomAuthClass = mcpCustomAuthClass;
    }
} 