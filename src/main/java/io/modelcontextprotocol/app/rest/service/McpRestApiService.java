/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.app.rest.response.ServerInfo;
import io.modelcontextprotocol.app.rest.util.McpRestTools;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.Servlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 提供Web服务接口，接收Swagger API JSON并动态启动MCP服务器
 */
public class McpRestApiService {

	private static final Logger logger = LoggerFactory.getLogger(McpRestApiService.class);

	// 端口号生成器，从基础端口开始递增
	private final AtomicInteger portCounter = new AtomicInteger(8080);

	// 基础上下文路径
	private static final String BASE_CONTEXT_PATH = "/mcp";

	// SSE端点
	private static final String SSE_ENDPOINT = "/mcp/sse";

	// 消息端点
	private static final String MESSAGE_ENDPOINT = "/mcp/message";

	// 存储运行中的服务器实例
	private final Map<String, ServerInstance> runningServers = new ConcurrentHashMap<>();

	// 对象序列化/反序列化器
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 从Swagger JSON创建并启动MCP服务器
	 * @param swaggerJson Swagger API的JSON字符串
	 * @param baseUrl API的基础URL
	 * @param serverName 服务器名称
	 * @param serverVersion 服务器版本
	 * @return 服务器信息，包含服务器ID和访问URL
	 */
	public ServerInfo createServerFromSwagger(String swaggerJson, String baseUrl, String serverName,
			String serverVersion) {
		// 生成唯一服务器ID
		String serverId = "server-" + System.currentTimeMillis();

		// TODO 分配端口号，扩展端口号分配策略
		int port = portCounter.getAndIncrement();
		if (port == 8080){
			port = portCounter.getAndIncrement();
		}

		// 创建上下文路径
		String contextPath = "";

		try {
			// 创建传输提供者
			HttpServletSseServerTransportProvider transportProvider = new HttpServletSseServerTransportProvider(
					objectMapper, MESSAGE_ENDPOINT, SSE_ENDPOINT);

			// 使用McpRestTools创建MCP服务器规范
			McpServer.SyncSpecification serverSpec = McpRestTools.createSyncServerFromSwagger(transportProvider,
					swaggerJson, baseUrl, serverName, serverVersion);

			// 构建MCP服务器
			McpSyncServer mcpServer = serverSpec.build();

			// 创建并启动Tomcat服务器
			Tomcat tomcat = createTomcatServer(contextPath, port, transportProvider);
			tomcat.start();

			// 存储服务器实例
			ServerInstance instance = new ServerInstance(serverId, tomcat, mcpServer);
			runningServers.put(serverId, instance);

			// 构建访问URL
			String serverUrl = "http://localhost:" + port + contextPath + SSE_ENDPOINT;

			logger.info("Started MCP server with ID '{}' at {}", serverId, serverUrl);

			return new ServerInfo(serverId, serverUrl, serverName, serverVersion);

		}
		catch (Exception e) {
			logger.error("Failed to create MCP server from Swagger", e);
			throw new RuntimeException("Failed to create MCP server: " + e.getMessage(), e);
		}
	}

	public String testCall(String baseUrl) {
		// 配置客户端
		McpClient.SyncSpec clientBuilder = McpClient
				.sync(HttpClientSseClientTransport.builder(baseUrl).sseEndpoint(SSE_ENDPOINT).build());
		try {
			// 创建客户端并初始化
			try (var client = clientBuilder.clientInfo(new McpSchema.Implementation("user-service", "1.0.0")).build()) {

				// 初始化客户端
				McpSchema.InitializeResult initResult = client.initialize();

				List<McpSchema.Tool> tools = client.listTools().tools();

				// 准备登录参数
				Map<String, Object> loginParams = Map.of("body", Map.of("source", "104", "tenantId", "1"));

				// 调用登录工具
				McpSchema.CallToolRequest loginRequest = new McpSchema.CallToolRequest("getAuthCodeImgUsingPOST", loginParams);

				// 执行工具调用
				McpSchema.CallToolResult result = client.callTool(loginRequest);

				// 获取响应文本
				String responseText = ((McpSchema.TextContent) result.content().get(0)).text();
				return responseText;
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Test failed", e);
		}
	}

	/**
	 * 停止并移除指定的MCP服务器
	 * @param serverId 服务器ID
	 * @return 是否成功停止
	 */
	public boolean stopServer(String serverId) {
		ServerInstance instance = runningServers.remove(serverId);

		if (instance == null) {
			logger.warn("No server found with ID '{}'", serverId);
			return false;
		}

		try {
			// 关闭MCP服务器
			instance.mcpServer.close();

			// 停止Tomcat服务器
			instance.tomcat.stop();
			instance.tomcat.destroy();

			logger.info("Stopped MCP server with ID '{}'", serverId);
			return true;
		}
		catch (Exception e) {
			logger.error("Failed to stop MCP server with ID '{}'", serverId, e);
			return false;
		}
	}

	/**
	 * 获取所有运行中的服务器信息
	 * @return 服务器ID到服务器信息的映射
	 */
	public Map<String, ServerInfo> getRunningServers() {
		Map<String, ServerInfo> result = new ConcurrentHashMap<>();

		runningServers.forEach((id, instance) -> {

			//String contextPath = BASE_CONTEXT_PATH + "/" + id;

			int port = instance.tomcat.getConnector().getPort();
			String serverUrl = "http://localhost:" + port + SSE_ENDPOINT;

			result.put(id, new ServerInfo(id, serverUrl, instance.mcpServer.getServerInfo().name(),
					instance.mcpServer.getServerInfo().version()));
		});

		return result;
	}

	/**
	 * 关闭所有运行中的服务器
	 */
	public void stopAllServers() {
		runningServers.keySet().forEach(this::stopServer);
	}

	/**
	 * 创建Tomcat服务器
	 */
	private Tomcat createTomcatServer(String contextPath, int port, Servlet servlet) {
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(port);

		String baseDir = System.getProperty("java.io.tmpdir");
		tomcat.setBaseDir(baseDir);

		Context context = tomcat.addContext(contextPath, baseDir);

		// 添加传输Servlet到Tomcat
		org.apache.catalina.Wrapper wrapper = context.createWrapper();
		wrapper.setName("mcpServlet");
		wrapper.setServlet(servlet);
		wrapper.setLoadOnStartup(1);
		wrapper.setAsyncSupported(true);
		context.addChild(wrapper);
		context.addServletMappingDecoded("/*", "mcpServlet");

		var connector = tomcat.getConnector();
		connector.setAsyncTimeout(3000);

		return tomcat;
	}

	/**
	 * 服务器实例内部类
	 */
	private static class ServerInstance {

		final String id;

		final Tomcat tomcat;

		final McpSyncServer mcpServer;

		ServerInstance(String id, Tomcat tomcat, McpSyncServer mcpServer) {
			this.id = id;
			this.tomcat = tomcat;
			this.mcpServer = mcpServer;
		}
	}
}