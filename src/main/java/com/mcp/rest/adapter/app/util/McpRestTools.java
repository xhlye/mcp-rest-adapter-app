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

/**
 * 提供静态工具方法，简化MCP REST API适配器的使用
 */
public final class McpRestTools {

	private McpRestTools() {
		// 工具类，不允许实例化
	}

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
				.swaggerJson(request.getSwaggerJson())
				.baseUrl(request.getBaseUrl());

			// 配置认证信息
			if (request.getBearerToken() != null) {
				adapterBuilder.bearerToken(request.getBearerToken());
			}
			else if (request.getUsername() != null && request.getPassword() != null) {
				adapterBuilder.basicAuth(request.getUsername(), request.getPassword());
			}
			else if (request.getApiKey() != null && request.getApiKeyName() != null && request.getApiKeyLocation() != null) {
				adapterBuilder.apiKey(request.getApiKey(), request.getApiKeyName(), request.getApiKeyLocation());
			}
			else if (request.getCustomAuthToken() != null) {
				adapterBuilder.customAuth(request.getCustomAuthToken());
			}

			// 添加自定义请求头
			if (request.getHeaders() != null) {
				adapterBuilder.headers(request.getHeaders());
			}

			// 构建适配器
			try (McpRestApiAdapter adapter = adapterBuilder.build()) {
				List<McpServerFeatures.SyncToolSpecification> tools = adapter.generateSyncToolSpecifications();

				McpServer.SyncSpecification server = McpServer.sync(transportProvider)
					.serverInfo(request.getServerName(), request.getServerVersion())
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
	 * 从Swagger JSON构建同步MCP服务器（兼容旧接口）
	 */
	public static McpServer.SyncSpecification createSyncServerFromSwagger(McpServerTransportProvider transportProvider,
			String swaggerJson, String baseUrl, String serverName, String serverVersion) {
		CreateServerRequest request = new CreateServerRequest();
		request.setSwaggerJson(swaggerJson);
		request.setBaseUrl(baseUrl);
		request.setServerName(serverName);
		request.setServerVersion(serverVersion);
		return createSyncServerFromSwagger(transportProvider, request);
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
	 * 从Swagger文件构建同步MCP服务器
	 * @param transportProvider MCP传输提供程序
	 * @param swaggerFilePath Swagger文件路径
	 * @param baseUrl API基础URL
	 * @param serverName 服务器名称
	 * @param serverVersion 服务器版本
	 * @return 配置好的MCP服务器构建器
	 */
	public static McpServer.SyncSpecification createSyncServerFromSwaggerFile(
			McpServerTransportProvider transportProvider, String swaggerFilePath, String baseUrl, String serverName,
			String serverVersion) {

		try (McpRestApiAdapter adapter = McpRestApiAdapter.builder()
			.swaggerFile(swaggerFilePath)
			.baseUrl(baseUrl)
			.build()) {

			List<McpServerFeatures.SyncToolSpecification> tools = adapter.generateSyncToolSpecifications();

			McpServer.SyncSpecification server = McpServer.sync(transportProvider)
				.serverInfo(serverName, serverVersion)
				.instructions("REST API tools provided by MCP REST Adapter");

			// 添加工具
			for (McpServerFeatures.SyncToolSpecification tool : tools) {
				server.tool(tool.tool(), tool.call());
			}

			return server;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to create MCP server from Swagger file", e);
		}
	}

	/**
	 * 从Swagger文件构建异步MCP服务器
	 * @param transportProvider MCP传输提供程序
	 * @param swaggerFilePath Swagger文件路径
	 * @param baseUrl API基础URL
	 * @param serverName 服务器名称
	 * @param serverVersion 服务器版本
	 * @return 配置好的MCP服务器构建器
	 */
	public static McpServer.AsyncSpecification createAsyncServerFromSwaggerFile(
			McpServerTransportProvider transportProvider, String swaggerFilePath, String baseUrl, String serverName,
			String serverVersion) {

		try (McpRestApiAdapter adapter = McpRestApiAdapter.builder()
			.swaggerFile(swaggerFilePath)
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
			throw new RuntimeException("Failed to create MCP server from Swagger file", e);
		}
	}

}