/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.util;

import io.modelcontextprotocol.app.rest.adapter.McpRestApiAdapter;
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
	 * @param swaggerJson Swagger JSON文档
	 * @param baseUrl API基础URL
	 * @param serverName 服务器名称
	 * @param serverVersion 服务器版本
	 * @return 配置好的MCP服务器构建器
	 */
	public static McpServer.SyncSpecification createSyncServerFromSwagger(McpServerTransportProvider transportProvider,
			String swaggerJson, String baseUrl, String serverName, String serverVersion) {

		try (McpRestApiAdapter adapter = McpRestApiAdapter.builder()
			.swaggerJson(swaggerJson)
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