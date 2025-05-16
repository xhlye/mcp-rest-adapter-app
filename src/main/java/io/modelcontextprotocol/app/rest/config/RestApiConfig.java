/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * REST API配置，包含连接到API所需的所有信息
 */
public class RestApiConfig {

	private final String baseUrl;

	private String authToken;

	private final Map<String, String> headers;

	private final int connectionTimeout;

	private final int readTimeout;

	private RestApiConfig(Builder builder) {
		this.baseUrl = builder.baseUrl;
		this.authToken = builder.authToken;
		this.headers = new HashMap<>(builder.headers);
		this.connectionTimeout = builder.connectionTimeout;
		this.readTimeout = builder.readTimeout;
	}

	/**
	 * 创建配置构建器
	 */
	public static Builder builder() {
		return new Builder();
	}

	// Getter方法
	public String getBaseUrl() {
		return baseUrl;
	}

	public String getAuthToken() {
		return authToken;
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
	 * 更新认证令牌
	 */
	public void updateAuthToken(String newToken) {
		this.authToken = newToken;
	}

	/**
	 * 配置构建器
	 */
	public static class Builder {

		private String baseUrl;

		private String authToken;

		private final Map<String, String> headers = new HashMap<>();

		private int connectionTimeout = 10000; // 默认10秒

		private int readTimeout = 30000; // 默认30秒

		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder authToken(String authToken) {
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
		return "RestApiConfig{" + "baseUrl='" + baseUrl + '\'' + ", headers=" + headers.keySet()
				+ ", connectionTimeout=" + connectionTimeout + ", readTimeout=" + readTimeout + ", hasAuthToken="
				+ (authToken != null && !authToken.isEmpty()) + '}';
	}

}