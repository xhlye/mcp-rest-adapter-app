/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.rest.adapter.app.config.RestApiConfig;
import com.mcp.rest.adapter.app.parser.ApiEndpoint;
import com.mcp.rest.adapter.app.parser.ApiParameter;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 代理REST API调用，将MCP工具调用转发到REST端点
 */
public class RestApiProxy implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(RestApiProxy.class);

	private final CloseableHttpClient httpClient;

	private final ObjectMapper objectMapper;

	private final RestApiConfig config;

	private final Map<String, ApiEndpoint> endpointMap;

	/**
	 * 创建REST API代理
	 * @param config REST API配置
	 * @param endpointMap 工具名称到API端点的映射
	 */
	public RestApiProxy(RestApiConfig config, Map<String, ApiEndpoint> endpointMap) {
		this(config, endpointMap, new ObjectMapper());
	}

	/**
	 * 创建REST API代理
	 * @param config REST API配置
	 * @param endpointMap 工具名称到API端点的映射
	 * @param objectMapper JSON序列化/反序列化器
	 */
	public RestApiProxy(RestApiConfig config, Map<String, ApiEndpoint> endpointMap, ObjectMapper objectMapper) {
		this.config = config;
		this.endpointMap = new HashMap<>(endpointMap);
		this.objectMapper = objectMapper;

		// 配置HTTP客户端
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.of(config.getConnectionTimeout(), TimeUnit.MILLISECONDS))
			.setResponseTimeout(Timeout.of(config.getReadTimeout(), TimeUnit.MILLISECONDS))
			.build();

		HttpClientBuilder builder = HttpClients.custom().setDefaultRequestConfig(requestConfig);

		this.httpClient = builder.build();
	}

	/**
	 * 执行API调用并返回结果
	 * @param toolName 工具名称
	 * @param arguments 工具参数
	 * @return 调用结果
	 */
	public CallToolResult executeCall(String toolName, Map<String, Object> arguments) {
		try {
			ApiEndpoint endpoint = endpointMap.get(toolName);
			if (endpoint == null) {
				logger.error("Unknown tool: {}", toolName);
				return CallToolResult.builder().addTextContent("Unknown tool: " + toolName).isError(true).build();
			}

			// 收集请求参数
			Map<String, Object> requestParams = prepareRequestParams(endpoint, arguments);

			// 构建完整URL
			String url = buildUrl(endpoint, requestParams);

			// 创建HTTP请求
			HttpUriRequestBase request = createRequest(endpoint.getMethod(), url);

			// 添加请求头
			addHeaders(request);

			// 添加请求体（如果需要）
			if (isMethodWithBody(endpoint.getMethod())) {
				addRequestBody(request, getBodyParams(endpoint, requestParams));
			}

			logger.debug("Executing API call: {} {}", endpoint.getMethod(), url);

			// 执行请求
			String response = httpClient.execute(request, httpResponse -> {
				int statusCode = httpResponse.getCode();
				String responseBody = null;

				// 读取响应内容
				if (httpResponse.getEntity() != null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					httpResponse.getEntity().writeTo(baos);
					responseBody = baos.toString();
				}

				// 处理错误响应
				if (statusCode >= 400) {
					logger.error("Error response from API: {} {}", statusCode, responseBody);
					return "Error: " + statusCode + " - " + (responseBody != null ? responseBody : "No response body");
				}

				logger.debug("API response: {}", responseBody);
				return responseBody;
			});

			return CallToolResult.builder().addTextContent(response).isError(false).build();

		}
		catch (Exception e) {
			logger.error("Error executing API call", e);
			return CallToolResult.builder()
				.addTextContent("Error executing API call: " + e.getMessage())
				.isError(true)
				.build();
		}
	}

	/**
	 * 预处理请求参数，从URL路径中提取路径参数
	 */
	private Map<String, Object> prepareRequestParams(ApiEndpoint endpoint, Map<String, Object> arguments) {
		Map<String, Object> result = new HashMap<>(arguments);

		// 检查是否缺少必需参数
		List<String> missingRequired = new ArrayList<>();
		for (ApiParameter param : endpoint.getParameters()) {
			if (param.isRequired() && !result.containsKey(param.getName())) {
				missingRequired.add(param.getName());
			}
		}

		if (!missingRequired.isEmpty()) {
			throw new IllegalArgumentException("Missing required parameters: " + String.join(", ", missingRequired));
		}

		return result;
	}

	/**
	 * 获取应放入请求体的参数
	 */
	private Map<String, Object> getBodyParams(ApiEndpoint endpoint, Map<String, Object> allParams) {
		// 对于有请求体的方法，需要判断哪些参数应该放入请求体
		Map<String, Object> bodyParams = new HashMap<>();
		Map<String, Object> remainingParams = new HashMap<>(allParams);

		// 将路径参数从请求体中移除
		for (ApiParameter param : endpoint.getParameters()) {
			if ("path".equals(param.getIn()) || "query".equals(param.getIn()) || "header".equals(param.getIn())) {
				// 这些参数不应该在请求体中
				remainingParams.remove(param.getName());
			}
			else if ("body".equals(param.getIn())) {
				Object value = allParams.get(param.getName());
				if (value != null) {
					// 对于body参数，直接使用其值作为请求体
					return value instanceof Map ? (Map<String, Object>) value : Map.of("value", value);
				}
			}
		}

		// 使用剩余的所有参数作为请求体
		return remainingParams;
	}

	/**
	 * 添加请求头
	 */
	private void addHeaders(HttpUriRequestBase request) {
		// 添加Content-Type
		request.addHeader("Content-Type", "application/json");

		// 添加Accept
		request.addHeader("Accept", "application/json");

		// 根据不同的授权类型添加认证信息
		switch (config.getAuthType()) {
			case BEARER_TOKEN:
				request.addHeader("Authorization", "Bearer " + config.getAuthToken());
				break;
			case BASIC_AUTH:
				String credentials = config.getUsername() + ":" + config.getPassword();
				String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
				request.addHeader("Authorization", "Basic " + encodedCredentials);
				break;
			case API_KEY:
				switch (config.getApiKeyLocation()) {
					case HEADER:
						request.addHeader(config.getApiKeyName(), config.getApiKey());
						break;
					case QUERY:
						// 查询参数在buildUrl方法中处理
						break;
					case COOKIE:
						request.addHeader("Cookie", config.getApiKeyName() + "=" + config.getApiKey());
						break;
				}
				break;
			case CUSTOM:
				request.addHeader("Authorization", config.getAuthToken());
				break;
		}

		// 添加其他头信息
		for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
			request.addHeader(header.getKey(), header.getValue());
		}
	}

	/**
	 * 添加请求体
	 */
	private void addRequestBody(HttpUriRequestBase request, Map<String, Object> arguments) throws IOException {
		// 将参数转换为JSON
		String json = objectMapper.writeValueAsString(arguments);

		// 添加到请求
		if (request instanceof HttpEntityContainer) {
			((HttpEntityContainer) request).setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		}
	}

	/**
	 * 检查HTTP方法是否包含请求体
	 */
	private boolean isMethodWithBody(String method) {
		return method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH");
	}

	/**
	 * 构建完整URL，替换路径参数，添加查询参数
	 */
	private String buildUrl(ApiEndpoint endpoint, Map<String, Object> arguments) {
		String baseUrl = config.getBaseUrl();
		String path = endpoint.getPath();

		// 如果路径以/开头，且基础URL也以/结尾，则移除路径开头的/
		if (path.startsWith("/") && baseUrl.endsWith("/")) {
			path = path.substring(1);
		}

		String url = baseUrl + path;

		// 替换路径参数
		Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
		Matcher matcher = pattern.matcher(url);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String paramName = matcher.group(1);
			Object value = arguments.get(paramName);

			if (value != null) {
				matcher.appendReplacement(sb, value.toString());
				arguments.remove(paramName); // 从参数中移除已使用的路径参数
			}
			else {
				throw new IllegalArgumentException("Missing required path parameter: " + paramName);
			}
		}
		matcher.appendTail(sb);

		// 构建查询字符串
		boolean hasQuery = sb.toString().contains("?");
		StringBuilder queryString = new StringBuilder();

		// 添加API Key（如果是查询参数）
		if (config.getAuthType() == RestApiConfig.AuthType.API_KEY 
				&& config.getApiKeyLocation() == RestApiConfig.ApiKeyLocation.QUERY) {
			queryString.append(hasQuery ? "&" : "?")
					  .append(config.getApiKeyName())
					  .append("=")
					  .append(config.getApiKey());
			hasQuery = true;
		}

		// 添加其他查询参数（对于GET请求）
		if (endpoint.getMethod().equals("GET") && !arguments.isEmpty()) {
			// 筛选出查询参数
			Map<String, Object> queryParams = arguments.entrySet()
				.stream()
				.filter(entry -> endpoint.getParameters()
					.stream()
					.anyMatch(p -> p.getName().equals(entry.getKey())
							&& ("query".equals(p.getIn()) || p.getIn() == null)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

			if (!queryParams.isEmpty()) {
				queryString.append(hasQuery ? "&" : "?");

				boolean first = true;
				for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
					if (!first) {
						queryString.append("&");
					}
					queryString.append(entry.getKey()).append("=").append(entry.getValue());
					first = false;
				}
			}
		}

		return sb.toString() + queryString.toString();
	}

	/**
	 * 创建HTTP请求
	 */
	private HttpUriRequestBase createRequest(String method, String url) {
		switch (method.toUpperCase()) {
			case "GET":
				return new HttpGet(url);
			case "POST":
				return new HttpPost(url);
			case "PUT":
				return new HttpPut(url);
			case "DELETE":
				return new HttpDelete(url);
			case "PATCH":
				return new HttpPatch(url);
			default:
				throw new IllegalArgumentException("Unsupported HTTP method: " + method);
		}
	}

	/**
	 * 获取API配置
	 */
	public RestApiConfig getConfig() {
		return this.config;
	}

	@Override
	public void close() throws IOException {
		this.httpClient.close();
	}

}