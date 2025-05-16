/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.app.rest.config.RestApiConfig;
import io.modelcontextprotocol.app.rest.parser.ApiEndpoint;
import io.modelcontextprotocol.app.rest.parser.ApiParameter;
import io.modelcontextprotocol.app.rest.parser.ApiResponse;
import io.modelcontextprotocol.app.rest.proxy.RestApiProxy;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RestApiProxy测试类 注意：这些测试依赖于外部API服务，如果API不可用，测试将被跳过
 */
public class RestApiProxyTest {

	private RestApiConfig config;

	private Map<String, ApiEndpoint> endpointMap;

	private ObjectMapper objectMapper;

	private boolean skipTests = false;

	@BeforeEach
	public void setup() {
		// 设置测试环境
		// 使用公共API进行测试: https://jsonplaceholder.typicode.com/ 是一个提供测试用RESTful API的服务
		objectMapper = new ObjectMapper();

		// 创建API配置
		config = RestApiConfig.builder()
			.baseUrl("https://jsonplaceholder.typicode.com/")
			.connectionTimeout(5000)
			.readTimeout(5000)
			.build();

		// 创建端点映射
		endpointMap = new HashMap<>();

		// 添加GET请求的端点 - 获取单个帖子
		ApiEndpoint getPostEndpoint = createGetPostEndpoint();
		endpointMap.put("getPost", getPostEndpoint);

		// 添加POST请求的端点 - 创建帖子
		ApiEndpoint createPostEndpoint = createCreatePostEndpoint();
		endpointMap.put("createPost", createPostEndpoint);

		// 添加带路径参数的GET请求端点 - 获取指定用户的帖子
		ApiEndpoint getUserPostsEndpoint = createGetUserPostsEndpoint();
		endpointMap.put("getUserPosts", getUserPostsEndpoint);

		// 检查API是否可用，如果不可用则跳过测试
		checkApiAvailability();
	}

	private void checkApiAvailability() {
		// 创建一个临时的RestApiProxy来检查API是否可用
		try (RestApiProxy tempProxy = new RestApiProxy(config, endpointMap, objectMapper)) {
			// 尝试获取第一篇帖子
			CallToolResult result = tempProxy.executeCall("getPost", Map.of("id", "1"));
			// 如果请求成功且响应不为空，则API可用
			skipTests = result.isError() || result.content().isEmpty();
		}
		catch (Exception e) {
			// 如果发生异常，则API不可用，跳过测试
			skipTests = true;
		}
	}

	private ApiEndpoint createGetPostEndpoint() {
		// 创建一个GET /posts/{id} 端点
		List<ApiParameter> parameters = List.of(new ApiParameter("id", "path", true, new StringSchema(), "Post ID"));

		// 创建响应模式
		Schema<?> responseSchema = new ObjectSchema();
		Map<String, Schema> properties = new HashMap<>();
		properties.put("id", new StringSchema());
		properties.put("title", new StringSchema());
		properties.put("body", new StringSchema());
		properties.put("userId", new StringSchema());
		responseSchema.setProperties(properties);

		return new ApiEndpoint("/posts/{id}", "GET", "getPost", "Get a post by ID", "Retrieve a post by its ID",
				parameters, new ApiResponse(Collections.singletonMap("response", responseSchema)));
	}

	private ApiEndpoint createCreatePostEndpoint() {
		// 创建一个POST /posts 端点
		Schema<?> requestSchema = new ObjectSchema();
		Map<String, Schema> properties = new HashMap<>();
		properties.put("title", new StringSchema());
		properties.put("body", new StringSchema());
		properties.put("userId", new StringSchema());
		requestSchema.setProperties(properties);

		List<ApiParameter> parameters = List.of(new ApiParameter("body", "body", true, requestSchema, "Post data"));

		return new ApiEndpoint("/posts", "POST", "createPost", "Create a post", "Create a new post", parameters,
				new ApiResponse(Collections.singletonMap("response", requestSchema)));
	}

	private ApiEndpoint createGetUserPostsEndpoint() {
		// 创建一个GET /users/{userId}/posts 端点
		List<ApiParameter> parameters = List
			.of(new ApiParameter("userId", "path", true, new StringSchema(), "User ID"));

		Schema<?> responseSchema = new ObjectSchema();

		return new ApiEndpoint("/users/{userId}/posts", "GET", "getUserPosts", "Get posts by user ID",
				"Retrieve all posts from a specific user", parameters,
				new ApiResponse(Collections.singletonMap("response", responseSchema)));
	}

	@Test
	public void testGetPostRequest() {
		// 如果API不可用，跳过测试
		assumeTrue(!skipTests, "API不可用，跳过测试");

		try (RestApiProxy proxy = new RestApiProxy(config, endpointMap, objectMapper)) {
			// 调用获取帖子的API
			Map<String, Object> args = Map.of("id", "1");
			CallToolResult result = proxy.executeCall("getPost", args);

			// 验证调用结果
			assertThat(result).isNotNull();
			assertThat(result.isError()).isFalse();
			assertThat(result.content()).isNotEmpty();

			// 获取响应文本并解析为JSON
			String responseText = ((TextContent) result.content().get(0)).text();

			// 验证响应中包含必要的字段
			assertThat(responseText).contains("id");
			assertThat(responseText).contains("title");
			assertThat(responseText).contains("body");
			assertThat(responseText).contains("userId");

			// 解析响应JSON
			Map<String, Object> responseMap = objectMapper.readValue(responseText, Map.class);

			// 验证特定ID的帖子是否正确返回
			assertThat(responseMap.get("id")).isEqualTo(1);

			// 打印响应结果供参考
			System.out.println("GET帖子响应: " + responseText);
		}
		catch (Exception e) {
			throw new RuntimeException("测试失败", e);
		}
	}

	@Test
	public void testCreatePostRequest() {
		// 如果API不可用，跳过测试
		assumeTrue(!skipTests, "API不可用，跳过测试");

		try (RestApiProxy proxy = new RestApiProxy(config, endpointMap, objectMapper)) {
			// 创建帖子参数
			Map<String, Object> postData = Map.of("title", "测试标题", "body", "这是一个测试内容", "userId", 1);

			Map<String, Object> args = Map.of("body", postData);

			// 调用创建帖子的API
			CallToolResult result = proxy.executeCall("createPost", args);

			// 验证调用结果
			assertThat(result).isNotNull();
			assertThat(result.isError()).isFalse();
			assertThat(result.content()).isNotEmpty();

			// 获取响应文本并解析为JSON
			String responseText = ((TextContent) result.content().get(0)).text();

			// 验证响应中包含必要的字段
			assertThat(responseText).contains("id");
			assertThat(responseText).contains("title");
			assertThat(responseText).contains("body");

			// 解析响应JSON
			Map<String, Object> responseMap = objectMapper.readValue(responseText, Map.class);

			// 验证响应中返回了我们提交的数据
			assertThat(responseMap.get("title")).isEqualTo("测试标题");
			assertThat(responseMap.get("body")).isEqualTo("这是一个测试内容");
			assertThat(responseMap.get("userId")).isEqualTo(1);

			// 打印响应结果供参考
			System.out.println("创建帖子响应: " + responseText);
		}
		catch (Exception e) {
			throw new RuntimeException("测试失败", e);
		}
	}

	@Test
	public void testGetUserPostsRequest() {
		// 如果API不可用，跳过测试
		assumeTrue(!skipTests, "API不可用，跳过测试");

		try (RestApiProxy proxy = new RestApiProxy(config, endpointMap, objectMapper)) {
			// 调用获取用户帖子的API
			Map<String, Object> args = Map.of("userId", "1");
			CallToolResult result = proxy.executeCall("getUserPosts", args);

			// 验证调用结果
			assertThat(result).isNotNull();
			assertThat(result.isError()).isFalse();
			assertThat(result.content()).isNotEmpty();

			// 获取响应文本
			String responseText = ((TextContent) result.content().get(0)).text();

			// 验证响应是一个数组
			assertThat(responseText).startsWith("[");
			assertThat(responseText).endsWith("]");

			// 解析响应JSON为List
			List<Map<String, Object>> posts = objectMapper.readValue(responseText, List.class);

			// 验证至少返回了一个帖子
			assertThat(posts).isNotEmpty();

			// 验证所有帖子都属于用户1
			for (Map<String, Object> post : posts) {
				assertThat(post.get("userId")).isEqualTo(1);
			}

			// 打印响应结果供参考（只打印前两个帖子，以免输出过多）
			System.out.println("用户帖子数量: " + posts.size());
			if (posts.size() > 0) {
				System.out.println("第一个帖子: " + posts.get(0));
			}
			if (posts.size() > 1) {
				System.out.println("第二个帖子: " + posts.get(1));
			}
		}
		catch (Exception e) {
			throw new RuntimeException("测试失败", e);
		}
	}

}