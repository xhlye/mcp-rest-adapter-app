/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.request;

import com.mcp.rest.adapter.app.config.RestApiConfig;

/**
 * API认证信息
 */
public class ApiAuth {
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
} 