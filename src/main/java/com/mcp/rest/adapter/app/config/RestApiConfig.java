/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * REST API配置，包含连接到API所需的所有信息
 */
public class RestApiConfig {

    /**
     * 授权类型枚举
     */
    public enum AuthType {
        NONE,           // 无需授权
        BEARER_TOKEN,   // Bearer Token
        BASIC_AUTH,     // Basic认证
        API_KEY,        // API Key
        CUSTOM         // 自定义授权
    }

    private final String baseUrl;
    private final AuthType authType;
    private String authToken;
    private String username;
    private String password;
    private String apiKey;
    private String apiKeyName;
    private ApiKeyLocation apiKeyLocation;
    private final Map<String, String> headers;
    private final int connectionTimeout;
    private final int readTimeout;

    /**
     * API Key放置位置
     */
    public enum ApiKeyLocation {
        HEADER,     // 请求头
        QUERY,      // 查询参数
        COOKIE      // Cookie
    }

    private RestApiConfig(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.authType = builder.authType;
        this.authToken = builder.authToken;
        this.username = builder.username;
        this.password = builder.password;
        this.apiKey = builder.apiKey;
        this.apiKeyName = builder.apiKeyName;
        this.apiKeyLocation = builder.apiKeyLocation;
        this.headers = new HashMap<>(builder.headers);
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter方法
    public String getBaseUrl() {
        return baseUrl;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public ApiKeyLocation getApiKeyLocation() {
        return apiKeyLocation;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * 更新认证信息
     */
    public void updateAuthInfo(String newToken) {
        this.authToken = newToken;
    }

    public void updateBasicAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void updateApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 配置构建器
     */
    public static class Builder {
        private String baseUrl;
        private AuthType authType = AuthType.NONE;
        private String authToken;
        private String username;
        private String password;
        private String apiKey;
        private String apiKeyName;
        private ApiKeyLocation apiKeyLocation;
        private final Map<String, String> headers = new HashMap<>();
        private int connectionTimeout = 10000; // 默认10秒
        private int readTimeout = 30000; // 默认30秒

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder bearerToken(String token) {
            this.authType = AuthType.BEARER_TOKEN;
            this.authToken = token;
            return this;
        }

        public Builder basicAuth(String username, String password) {
            this.authType = AuthType.BASIC_AUTH;
            this.username = username;
            this.password = password;
            return this;
        }

        public Builder apiKey(String apiKey, String keyName, ApiKeyLocation location) {
            this.authType = AuthType.API_KEY;
            this.apiKey = apiKey;
            this.apiKeyName = keyName;
            this.apiKeyLocation = location;
            return this;
        }

        public Builder customAuth(String authToken) {
            this.authType = AuthType.CUSTOM;
            this.authToken = authToken;
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public Builder connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public RestApiConfig build() {
            validate();
            return new RestApiConfig(this);
        }

        private void validate() {
            Objects.requireNonNull(baseUrl, "Base URL is required");

            if (baseUrl.isEmpty()) {
                throw new IllegalArgumentException("Base URL cannot be empty");
            }

            // 确保baseUrl以"/"结尾
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/";
            }

            // 验证授权相关配置
            switch (authType) {
                case BEARER_TOKEN:
                case CUSTOM:
                    if (authToken == null || authToken.isEmpty()) {
                        throw new IllegalArgumentException("Auth token is required for " + authType);
                    }
                    break;
                case BASIC_AUTH:
                    if (username == null || username.isEmpty() || password == null) {
                        throw new IllegalArgumentException("Username and password are required for Basic Auth");
                    }
                    break;
                case API_KEY:
                    if (apiKey == null || apiKey.isEmpty() || apiKeyName == null || apiKeyName.isEmpty() || apiKeyLocation == null) {
                        throw new IllegalArgumentException("API key, key name and location are required for API Key auth");
                    }
                    break;
            }

            if (connectionTimeout <= 0) {
                throw new IllegalArgumentException("Connection timeout must be positive");
            }

            if (readTimeout <= 0) {
                throw new IllegalArgumentException("Read timeout must be positive");
            }
        }
    }

    @Override
    public String toString() {
        return "RestApiConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", authType=" + authType +
                ", headers=" + headers.keySet() +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                '}';
    }
}