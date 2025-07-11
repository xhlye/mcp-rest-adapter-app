/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.request;

import com.mcp.rest.adapter.app.config.RestApiConfig;
import java.util.Map;
import java.util.HashMap;

/**
 * 创建MCP服务器的请求体
 */
public class CreateServerRequest {
    /**
     * Swagger API的JSON字符串，定义了API的结构和端点
     */
    private String swaggerJson;
    
    /**
     * API的基础URL，指向实际服务的地址
     */
    private String baseUrl;
    
    /**
     * 服务器名称，用于标识MCP服务器
     */
    private String serverName;
    
    /**
     * 服务器版本，用于版本控制
     */
    private String serverVersion;

    /**
     * Bearer Token认证令牌
     */
    private String bearerToken;

    /**
     * Basic认证用户名
     */
    private String username;

    /**
     * Basic认证密码
     */
    private String password;

    /**
     * API Key值
     */
    private String apiKey;

    /**
     * API Key名称
     */
    private String apiKeyName;

    /**
     * API Key位置(HEADER/QUERY/COOKIE)
     */
    private RestApiConfig.ApiKeyLocation apiKeyLocation;

    /**
     * 自定义认证令牌
     */
    private String customAuthToken;

    /**
     * 自定义请求头
     */
    private Map<String, String> headers = new HashMap<>();
    
    public String getSwaggerJson() {
        return swaggerJson;
    }
    
    public void setSwaggerJson(String swaggerJson) {
        this.swaggerJson = swaggerJson;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public String getServerVersion() {
        return serverVersion;
    }
    
    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    public RestApiConfig.ApiKeyLocation getApiKeyLocation() {
        return apiKeyLocation;
    }

    public void setApiKeyLocation(RestApiConfig.ApiKeyLocation apiKeyLocation) {
        this.apiKeyLocation = apiKeyLocation;
    }

    public String getCustomAuthToken() {
        return customAuthToken;
    }

    public void setCustomAuthToken(String customAuthToken) {
        this.customAuthToken = customAuthToken;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }
} 