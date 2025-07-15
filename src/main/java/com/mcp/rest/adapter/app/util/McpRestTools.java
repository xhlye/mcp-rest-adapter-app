/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.util;

import com.mcp.rest.adapter.app.adapter.McpRestApiAdapter;
import com.mcp.rest.adapter.app.request.CreateServerRequest;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 提供静态工具方法，简化MCP REST API适配器的使用
 */
public final class McpRestTools {

	/**
	 * 从Swagger JSON构建同步MCP服务器
	 * @param transportProvider MCP传输提供程序
	 * @param request 创建服务器的请求参数，包含认证信息
	 * @return 配置好的MCP服务器构建器
	 */
	public static McpServer.SyncSpecification createSyncServerFromSwagger(McpServerTransportProvider transportProvider,
			CreateServerRequest request) {

		try {
			// 创建适配器构建器
			McpRestApiAdapter.Builder adapterBuilder = McpRestApiAdapter.builder()
				.swaggerJson(request.getServerInfo().getSwaggerJson())
				.baseUrl(request.getServerInfo().getBaseUrl());

			// 配置认证信息
			if (request.getApiAuth().getBearerToken() != null) {
				adapterBuilder.bearerToken(request.getApiAuth().getBearerToken());
			}
			else if (request.getApiAuth().getUsername() != null && request.getApiAuth().getPassword() != null) {
				adapterBuilder.basicAuth(request.getApiAuth().getUsername(), request.getApiAuth().getPassword());
			}
			else if (request.getApiAuth().getApiKey() != null && request.getApiAuth().getApiKeyName() != null && request.getApiAuth().getApiKeyLocation() != null) {
				adapterBuilder.apiKey(request.getApiAuth().getApiKey(), request.getApiAuth().getApiKeyName(), request.getApiAuth().getApiKeyLocation());
			}
			else if (request.getApiAuth().getCustomAuthToken() != null) {
				adapterBuilder.customAuth(request.getApiAuth().getCustomAuthToken());
			}

			// 添加自定义请求头
			if (request.getServerInfo().getHeaders() != null) {
				adapterBuilder.headers(request.getServerInfo().getHeaders());
			}

			// 构建适配器
			try (McpRestApiAdapter adapter = adapterBuilder.build()) {
				List<McpServerFeatures.SyncToolSpecification> tools = adapter.generateSyncToolSpecifications();

				McpServer.SyncSpecification server = McpServer.sync(transportProvider)
					.serverInfo(request.getServerInfo().getServerName(), request.getServerInfo().getServerVersion())
					.instructions("REST API tools provided by MCP REST Adapter");

				// 添加工具
				for (McpServerFeatures.SyncToolSpecification tool : tools) {
					server.tool(tool.tool(), tool.call());
				}

				return server;
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create MCP server from Swagger", e);
		}
	}

	/**
	 * 从Swagger JSON构建异步MCP服务器
	 * @param transportProvider MCP传输提供程序
	 * @param swaggerJson Swagger JSON文档
	 * @param baseUrl API基础URL
	 * @param serverName 服务器名称
	 * @param serverVersion 服务器版本
	 * @return 配置好的MCP服务器构建器
	 */
	public static McpServer.AsyncSpecification createAsyncServerFromSwagger(
			McpServerTransportProvider transportProvider, String swaggerJson, String baseUrl, String serverName,
			String serverVersion) {

		try (McpRestApiAdapter adapter = McpRestApiAdapter.builder()
			.swaggerJson(swaggerJson)
			.baseUrl(baseUrl)
			.build()) {

			List<McpServerFeatures.AsyncToolSpecification> tools = adapter.generateAsyncToolSpecifications();

			McpServer.AsyncSpecification server = McpServer.async(transportProvider)
				.serverInfo(serverName, serverVersion)
				.instructions("REST API tools provided by MCP REST Adapter");

			// 添加工具
			for (McpServerFeatures.AsyncToolSpecification tool : tools) {
				server.tool(tool.tool(), tool.call());
			}

			return server;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create MCP server from Swagger", e);
		}
	}

	/**
	 * 获取服务器URL，根据请求动态构建
	 * @param request HTTP请求
	 * @param port 端口号
	 * @param path 路径
	 * @return 完整的URL
	 */
	public static String getServerUrl(HttpServletRequest request, int port, String path) {
		// 获取请求的协议（http或https）
		String scheme = request.getScheme();
		// 获取服务器名称（主机名）
		String serverName = request.getServerName();
		// 使用提供的端口或请求的端口
		int serverPort = port > 0 ? port : request.getServerPort();

		// 构建URL
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);

		// 只有当端口不是默认端口时才添加端口号
		if ((scheme.equals("http") && serverPort != 80) ||
			(scheme.equals("https") && serverPort != 443)) {
			url.append(":").append(serverPort);
		}

		// 添加路径
		if (path != null && !path.isEmpty()) {
			if (!path.startsWith("/")) {
				url.append("/");
			}
			url.append(path);
		}

		return url.toString();
	}

}