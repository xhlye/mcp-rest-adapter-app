package com.mcp.rest.adapter.app.ratelimit;

import com.mcp.rest.adapter.app.config.McpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于TPS的限流器实现
 */
public class TpsRateLimiter implements RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(TpsRateLimiter.class);
    
    // 默认获取许可超时时间（毫秒）
    private static final long DEFAULT_ACQUIRE_TIMEOUT_MS = 500;
    
    // 服务ID与配置的映射
    private final Map<String, McpServerConfig> serverConfigs;
    
    // 服务ID与信号量的映射，用于控制并发请求数
    private final Map<String, Semaphore> concurrentLimiters = new ConcurrentHashMap<>();
    
    // 服务ID与当前活跃请求数的映射
    private final Map<String, AtomicInteger> activeRequests = new ConcurrentHashMap<>();
    
    // 服务ID与令牌桶的映射，用于控制TPS
    private final Map<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    
    // 客户端标识与服务ID的映射，用于释放资源
    private final Map<String, String> clientServerMap = new ConcurrentHashMap<>();
    
    public TpsRateLimiter(Map<String, McpServerConfig> serverConfigs) {
        this.serverConfigs = serverConfigs;
        
        // 初始化每个服务的限流器
        for (Map.Entry<String, McpServerConfig> entry : serverConfigs.entrySet()) {
            String serverId = entry.getKey();
            McpServerConfig config = entry.getValue();
            
            if (config.isEnableRateLimiting()) {
                // 创建并发请求限制器
                concurrentLimiters.put(serverId, new Semaphore(config.getMaxConcurrentRequests()));
                
                // 创建活跃请求计数器
                activeRequests.put(serverId, new AtomicInteger(0));
                
                // 创建令牌桶限流器
                tokenBuckets.put(serverId, new TokenBucket(config.getMaxTps()));
                
                logger.info("为服务 {} 初始化限流器: TPS={}, 最大并发={}", 
                        serverId, config.getMaxTps(), config.getMaxConcurrentRequests());
            }
        }
    }
    
    @Override
    public boolean tryAcquire(String serverId, String clientId) {
        McpServerConfig config = serverConfigs.get(serverId);
        if (config == null || !config.isEnableRateLimiting()) {
            // 如果没有配置或未启用限流，直接允许访问
            return true;
        }
        
        // 获取限流器
        Semaphore concurrentLimiter = concurrentLimiters.get(serverId);
        AtomicInteger activeCount = activeRequests.get(serverId);
        TokenBucket tokenBucket = tokenBuckets.get(serverId);
        
        if (concurrentLimiter == null || activeCount == null || tokenBucket == null) {
            logger.warn("服务 {} 的限流器未初始化", serverId);
            return true;
        }
        
        try {
            // 先检查TPS限制
            if (!tokenBucket.tryConsume()) {
                logger.warn("服务 {} 超过TPS限制", serverId);
                return false;
            }
            
            // 再检查并发请求限制
            if (!concurrentLimiter.tryAcquire(DEFAULT_ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                logger.warn("服务 {} 超过并发请求限制", serverId);
                // 如果获取失败，需要归还之前消费的令牌
                tokenBucket.returnToken();
                return false;
            }
            
            // 记录客户端与服务的关联，用于后续释放资源
            clientServerMap.put(clientId, serverId);
            
            // 增加活跃请求计数
            activeCount.incrementAndGet();
            
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取访问许可被中断", e);
            return false;
        }
    }
    
    @Override
    public void release(String serverId, String clientId) {
        // 如果没有指定serverId，则尝试从映射中获取
        if (serverId == null || serverId.isEmpty()) {
            serverId = clientServerMap.remove(clientId);
            if (serverId == null) {
                logger.warn("找不到客户端 {} 对应的服务ID", clientId);
                return;
            }
        }
        
        McpServerConfig config = serverConfigs.get(serverId);
        if (config == null || !config.isEnableRateLimiting()) {
            return;
        }
        
        // 获取限流器
        Semaphore concurrentLimiter = concurrentLimiters.get(serverId);
        AtomicInteger activeCount = activeRequests.get(serverId);
        
        if (concurrentLimiter == null || activeCount == null) {
            return;
        }
        
        // 释放并发请求许可
        concurrentLimiter.release();
        
        // 减少活跃请求计数
        activeCount.decrementAndGet();
    }
    
    @Override
    public double getUsageRate(String serverId) {
        McpServerConfig config = serverConfigs.get(serverId);
        if (config == null || !config.isEnableRateLimiting()) {
            return 0.0;
        }
        
        AtomicInteger activeCount = activeRequests.get(serverId);
        if (activeCount == null) {
            return 0.0;
        }
        
        // 计算当前使用率
        int currentActive = activeCount.get();
        int maxConcurrent = config.getMaxConcurrentRequests();
        
        return (double) currentActive / maxConcurrent;
    }
    
    /**
     * 令牌桶实现，用于控制TPS
     */
    private static class TokenBucket {
        // 最大令牌数（等于最大TPS）
        private final int maxTokens;
        
        // 当前可用令牌数
        private int availableTokens;
        
        // 上次补充令牌的时间（纳秒）
        private long lastRefillTime;
        
        // 每个令牌生成间隔（纳秒）
        private final long refillIntervalNanos;
        
        public TokenBucket(int tps) {
            this.maxTokens = tps;
            this.availableTokens = tps;
            this.lastRefillTime = System.nanoTime();
            // 计算令牌生成间隔（纳秒）
            this.refillIntervalNanos = TimeUnit.SECONDS.toNanos(1) / tps;
        }
        
        /**
         * 尝试消费一个令牌
         * @return 如果成功消费返回true，否则返回false
         */
        public synchronized boolean tryConsume() {
            refill();
            
            if (availableTokens > 0) {
                availableTokens--;
                return true;
            }
            
            return false;
        }
        
        /**
         * 归还一个令牌（用于回滚）
         */
        public synchronized void returnToken() {
            if (availableTokens < maxTokens) {
                availableTokens++;
            }
        }
        
        /**
         * 根据时间流逝补充令牌
         */
        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTime;
            
            // 计算应该生成的令牌数
            int tokensToAdd = (int) (elapsed / refillIntervalNanos);
            
            if (tokensToAdd > 0) {
                availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
} 