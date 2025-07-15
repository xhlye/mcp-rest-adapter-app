/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.request;

/**
 * 性能配置
 */
public class PerformanceConfig {
    /**
     * 是否启用限流
     */
    private boolean enableRateLimiting = false;
    
    /**
     * 最大TPS (每秒事务处理数)
     */
    private int maxTps = 100;
    
    /**
     * 最大并发请求数
     */
    private int maxConcurrentRequests = 50;
    
    /**
     * 请求超时时间(毫秒)
     */
    private int requestTimeoutMs = 30000;
    
    public boolean isEnableRateLimiting() {
        return enableRateLimiting;
    }
    
    public void setEnableRateLimiting(boolean enableRateLimiting) {
        this.enableRateLimiting = enableRateLimiting;
    }
    
    public int getMaxTps() {
        return maxTps;
    }
    
    public void setMaxTps(int maxTps) {
        this.maxTps = maxTps;
    }
    
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }
    
    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }
    
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }
    
    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }
} 