package com.mcp.rest.adapter.app.ratelimit;

/**
 * 限流器接口
 */
public interface RateLimiter {
    /**
     * 尝试获取访问许可
     * @param serverId 服务ID
     * @param clientId 客户端标识
     * @return 如果允许访问返回true，否则返回false
     */
    boolean tryAcquire(String serverId, String clientId);
    
    /**
     * 释放访问许可
     * @param serverId 服务ID
     * @param clientId 客户端标识
     */
    void release(String serverId, String clientId);
    
    /**
     * 获取当前使用率
     * @param serverId 服务ID
     * @return 使用率，范围0.0-1.0
     */
    double getUsageRate(String serverId);
} 