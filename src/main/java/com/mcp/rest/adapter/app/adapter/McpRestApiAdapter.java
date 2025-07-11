/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.rest.adapter.app.config.RestApiConfig;
import com.mcp.rest.adapter.app.generator.McpToolGenerator;
import com.mcp.rest.adapter.app.parser.ApiEndpoint;
import com.mcp.rest.adapter.app.parser.SwaggerParser;
import com.mcp.rest.adapter.app.proxy.RestApiProxy;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP REST API适配器 - 主类 用于将REST API转换为MCP工具
 */
public class McpRestApiAdapter implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(McpRestApiAdapter.class);

	private final SwaggerParser swaggerParser;

	private final McpToolGenerator toolGenerator;

	private final RestApiProxy apiProxy;

	private final List<ApiEndpoint> endpoints;

	private final Map<String, ApiEndpoint> endpointMap;

	private final ObjectMapper objectMapper;

	/**
	 * 私有构造函数，通过Builder创建实例
	 */
	private McpRestApiAdapter(String swaggerJson, RestApiConfig config, String toolNamePrefix) {
		this.objectMapper = new ObjectMapper();
		this.swaggerParser = new SwaggerParser(swaggerJson);
		this.endpoints = swaggerParser.extractEndpoints();
		this.toolGenerator = new McpToolGenerator(toolNamePrefix);

		// 构建工具名称到端点的映射
		Map<String, ApiEndpoint> tempMap = new HashMap<>();
		for (ApiEndpoint endpoint : endpoints) {
			String toolName = toolGenerator.generateToolName(endpoint);
			tempMap.put(toolName, endpoint);
		}
		this.endpointMap = tempMap;

		this.apiProxy = new RestApiProxy(config, endpointMap, objectMapper);

		logger.info("Initialized MCP REST API adapter with {} endpoints", endpoints.size());
	}

	/**
	 * 创建构建器
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 获取所有工具
	 */
	public List<Tool> generateTools() {
		return toolGenerator.generateTools(endpoints);
	}

	/**
	 * 生成MCP同步工具规范列表
	 */
	public List<McpServerFeatures.SyncToolSpecification> generateSyncToolSpecifications() {
		List<Tool> tools = generateTools();
		RestApiConfig config = this.apiProxy.getConfig();

		return tools.stream().map(tool -> new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
			// 为每次调用创建新的代理，避免连接池关闭问题
			try (RestApiProxy requestProxy = new RestApiProxy(config, endpointMap, objectMapper)) {
				return requestProxy.executeCall(tool.name(), args);
			}
			catch (Exception e) {
				logger.error("Error executing tool call: {}", e.getMessage(), e);
				return CallToolResult.builder().addTextContent("Error: " + e.getMessage()).isError(true).build();
			}
		})).collect(Collectors.toList());
	}

	/**
	 * 生成MCP异步工具规范列表
	 */
	public List<McpServerFeatures.AsyncToolSpecification> generateAsyncToolSpecifications() {
		List<Tool> tools = generateTools();
		RestApiConfig config = this.apiProxy.getConfig();

		return tools.stream()
			.map(tool -> new McpServerFeatures.AsyncToolSpecification(tool,
					(exchange, args) -> Mono.fromCallable(() -> {
						// 为每次调用创建新的代理，避免连接池关闭问题
						try (RestApiProxy requestProxy = new RestApiProxy(config, endpointMap, objectMapper)) {
							return requestProxy.executeCall(tool.name(), args);
						}
						catch (Exception e) {
							logger.error("Error executing tool call: {}", e.getMessage(), e);
							return CallToolResult.builder()
								.addTextContent("Error: " + e.getMessage())
								.isError(true)
								.build();
						}
					})))
			.collect(Collectors.toList());
	}

	/**
	 * 关闭适配器释放资源
	 */
	@Override
	public void close() throws Exception {
		if (apiProxy != null) {
			apiProxy.close();
		}
	}

	/**
	 * 适配器构建器
	 */
	public static class Builder {

		private String swaggerJson;

		private String swaggerFilePath;

		private RestApiConfig.Builder configBuilder = RestApiConfig.builder();

		private String toolNamePrefix = "";

		public Builder swaggerJson(String swaggerJson) {
			this.swaggerJson = swaggerJson;
			return this;
		}

		public Builder swaggerFile(String filePath) {
			this.swaggerFilePath = filePath;
			return this;
		}

		public Builder baseUrl(String baseUrl) {
			this.configBuilder.baseUrl(baseUrl);
			return this;
		}

		/**
		 * 设置Bearer Token认证
		 */
		public Builder bearerToken(String token) {
			this.configBuilder.bearerToken(token);
			return this;
		}

		/**
		 * 设置Basic认证
		 */
		public Builder basicAuth(String username, String password) {
			this.configBuilder.basicAuth(username, password);
			return this;
		}

		/**
		 * 设置API Key认证
		 */
		public Builder apiKey(String apiKey, String keyName, RestApiConfig.ApiKeyLocation location) {
			this.configBuilder.apiKey(apiKey, keyName, location);
			return this;
		}

		/**
		 * 设置自定义认证
		 */
		public Builder customAuth(String authToken) {
			this.configBuilder.customAuth(authToken);
			return this;
		}

		/**
		 * 添加自定义请求头
		 */
		public Builder addHeader(String name, String value) {
			this.configBuilder.addHeader(name, value);
			return this;
		}

		/**
		 * 批量添加请求头
		 */
		public Builder headers(Map<String, String> headers) {
			this.configBuilder.headers(headers);
			return this;
		}

		public Builder connectionTimeout(int connectionTimeout) {
			this.configBuilder.connectionTimeout(connectionTimeout);
			return this;
		}

		public Builder readTimeout(int readTimeout) {
			this.configBuilder.readTimeout(readTimeout);
			return this;
		}

		public Builder toolNamePrefix(String prefix) {
			this.toolNamePrefix = prefix != null ? prefix : "";
			return this;
		}

		public McpRestApiAdapter build() {
			// 验证必要参数
			if ((swaggerJson == null || swaggerJson.isEmpty())
					&& (swaggerFilePath == null || swaggerFilePath.isEmpty())) {
				throw new IllegalArgumentException("Either swaggerJson or swaggerFilePath must be provided");
			}

			// 获取Swagger文档
			String finalSwaggerJson = swaggerJson;
			if (finalSwaggerJson == null && swaggerFilePath != null) {
				try {
					// 从文件加载
					finalSwaggerJson = Files.readString(Paths.get(swaggerFilePath));
				}
				catch (IOException e) {
					throw new IllegalArgumentException("Failed to read Swagger file: " + swaggerFilePath, e);
				}
			}

			return new McpRestApiAdapter(finalSwaggerJson, configBuilder.build(), toolNamePrefix);
		}

	}

}