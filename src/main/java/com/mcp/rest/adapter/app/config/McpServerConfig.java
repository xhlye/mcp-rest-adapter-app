package com.mcp.rest.adapter.app.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MCP服务器配置类，包含鉴权和流量控制设置
 */
public class McpServerConfig {

    /**
     * 鉴权类型枚举
     */
    public enum AuthType {
        NONE,           // 无需鉴权
        API_KEY,        // API Key鉴权
        JWT_TOKEN,      // JWT Token鉴权
        CUSTOM          // 自定义鉴权
    }

    // 鉴权配置
    private final AuthType authType;
    private final Map<String, String> apiKeys;  // API Key映射表 (keyId -> secretKey)
    private final String jwtSecret;             // JWT密钥
    private final String customAuthClass;       // 自定义鉴权实现类名

    // 流量控制配置
    private final int maxTps;                   // 最大TPS (每秒事务处理数)
    private final int maxConcurrentRequests;    // 最大并发请求数
    private final int requestTimeoutMs;         // 请求超时时间(毫秒)
    private final boolean enableRateLimiting;   // 是否启用限流

    private McpServerConfig(Builder builder) {
        this.authType = builder.authType;
        this.apiKeys = new HashMap<>(builder.apiKeys);
        this.jwtSecret = builder.jwtSecret;
        this.customAuthClass = builder.customAuthClass;
        this.maxTps = builder.maxTps;
        this.maxConcurrentRequests = builder.maxConcurrentRequests;
        this.requestTimeoutMs = builder.requestTimeoutMs;
        this.enableRateLimiting = builder.enableRateLimiting;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter方法
    public AuthType getAuthType() {
        return authType;
    }

    public Map<String, String> getApiKeys() {
        return Collections.unmodifiableMap(apiKeys);
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public String getCustomAuthClass() {
        return customAuthClass;
    }

    public int getMaxTps() {
        return maxTps;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public boolean isEnableRateLimiting() {
        return enableRateLimiting;
    }

    /**
     * 配置构建器
     */
    public static class Builder {
        private AuthType authType = AuthType.NONE;
        private final Map<String, String> apiKeys = new HashMap<>();
        private String jwtSecret;
        private String customAuthClass;
        private int maxTps = 100;                  // 默认每秒100请求
        private int maxConcurrentRequests = 50;    // 默认最大50个并发请求
        private int requestTimeoutMs = 30000;      // 默认30秒超时
        private boolean enableRateLimiting = false; // 默认不启用限流

        public Builder noAuth() {
            this.authType = AuthType.NONE;
            return this;
        }

        public Builder apiKeyAuth() {
            this.authType = AuthType.API_KEY;
            return this;
        }

        public Builder addApiKey(String keyId, String secretKey) {
            this.apiKeys.put(keyId, secretKey);
            return this;
        }

        public Builder jwtAuth(String secret) {
            this.authType = AuthType.JWT_TOKEN;
            this.jwtSecret = secret;
            return this;
        }

        public Builder customAuth(String className) {
            this.authType = AuthType.CUSTOM;
            this.customAuthClass = className;
            return this;
        }

        public Builder maxTps(int tps) {
            this.maxTps = tps;
            return this;
        }

        public Builder maxConcurrentRequests(int maxRequests) {
            this.maxConcurrentRequests = maxRequests;
            return this;
        }

        public Builder requestTimeout(int timeoutMs) {
            this.requestTimeoutMs = timeoutMs;
            return this;
        }

        public Builder enableRateLimiting(boolean enable) {
            this.enableRateLimiting = enable;
            return this;
        }

        public McpServerConfig build() {
            validate();
            return new McpServerConfig(this);
        }

        private void validate() {
            // 验证鉴权相关配置
            switch (authType) {
                case API_KEY:
                    if (apiKeys.isEmpty()) {
                        throw new IllegalArgumentException("至少需要配置一个API Key");
                    }
                    break;
                case JWT_TOKEN:
                    if (jwtSecret == null || jwtSecret.isEmpty()) {
                        throw new IllegalArgumentException("JWT鉴权需要提供密钥");
                    }
                    break;
                case CUSTOM:
                    if (customAuthClass == null || customAuthClass.isEmpty()) {
                        throw new IllegalArgumentException("自定义鉴权需要提供实现类名");
                    }
                    break;
            }

            // 验证流量控制配置
            if (enableRateLimiting) {
                if (maxTps <= 0) {
                    throw new IllegalArgumentException("最大TPS必须为正数");
                }
                if (maxConcurrentRequests <= 0) {
                    throw new IllegalArgumentException("最大并发请求数必须为正数");
                }
                if (requestTimeoutMs <= 0) {
                    throw new IllegalArgumentException("请求超时时间必须为正数");
                }
            }
        }
    }

    @Override
    public String toString() {
        return "McpServerConfig{" +
                "authType=" + authType +
                ", apiKeysCount=" + apiKeys.size() +
                ", enableRateLimiting=" + enableRateLimiting +
                ", maxTps=" + maxTps +
                ", maxConcurrentRequests=" + maxConcurrentRequests +
                ", requestTimeoutMs=" + requestTimeoutMs +
                '}';
    }
} 