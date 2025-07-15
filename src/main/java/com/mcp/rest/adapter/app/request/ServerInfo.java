/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.request;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务基本信息
 */
public class ServerInfo {
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
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }
} 