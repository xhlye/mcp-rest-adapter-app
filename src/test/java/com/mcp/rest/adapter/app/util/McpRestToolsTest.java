/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * McpRestTools测试类
 */
public class McpRestToolsTest {

	@TempDir
	Path tempDir;

	private static final int PORT = 8083;

	private static final String SSE_ENDPOINT = "/mcp/sse";

	private static final String MESSAGE_ENDPOINT = "/mcp/message";

	private HttpServletSseServerTransportProvider transportProvider;

	private Tomcat tomcat;

	private McpClient.SyncSpec clientBuilder;

	@BeforeEach
	public void setup() {
		// 创建并配置传输提供者
		transportProvider = HttpServletSseServerTransportProvider.builder()
			.objectMapper(new ObjectMapper())
			.messageEndpoint(MESSAGE_ENDPOINT)
			.sseEndpoint(SSE_ENDPOINT)
			.build();

		// 启动Tomcat服务器
		tomcat = TomcatTestUtil.createTomcatServer("", PORT, transportProvider);
		try {
			tomcat.start();
			assertThat(tomcat.getServer().getState() == LifecycleState.STARTED).isTrue();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to start Tomcat", e);
		}

		// 配置客户端
		this.clientBuilder = McpClient
			.sync(HttpClientSseClientTransport.builder("http://localhost:" + PORT).sseEndpoint(SSE_ENDPOINT).build());
	}

	@AfterEach
	public void tearDown() {
		if (transportProvider != null) {
			transportProvider.closeGracefully().block();
		}
		if (tomcat != null) {
			try {
				tomcat.stop();
				tomcat.destroy();
			}
			catch (LifecycleException e) {
				throw new RuntimeException("Failed to stop Tomcat", e);
			}
		}
	}

	@Test
	public void testCreateSyncServerFromSwagger() {
		// 创建简单的Swagger JSON - 这是一个简化的Petstore API定义
		String swaggerJson = "{\n" +
				"    \"swagger\": \"2.0\",\n" +
				"    \"info\": {\n" +
				"        \"description\": \"Api Documentation\",\n" +
				"        \"version\": \"1.0\",\n" +
				"        \"title\": \"Api Documentation\",\n" +
				"        \"termsOfService\": \"urn:tos\",\n" +
				"        \"contact\": {},\n" +
				"        \"license\": {\n" +
				"            \"name\": \"Apache 2.0\",\n" +
				"            \"url\": \"http://www.apache.org/licenses/LICENSE-2.0\"\n" +
				"        }\n" +
				"    },\n" +
				"    \"host\": \"api.chongqing-test.business.xy\",\n" +
				"    \"basePath\": \"/user-service\",\n" +
				"    \"tags\": [\n" +
				"        {\n" +
				"            \"name\": \"账号Controller\",\n" +
				"            \"description\": \"Account Controller\"\n" +
				"        }\n" +
				"    ],\n" +
				"    \"paths\": {\n" +
				"        \"/account/login\": {\n" +
				"            \"post\": {\n" +
				"                \"tags\": [\n" +
				"                    \"账号Controller\"\n" +
				"                ],\n" +
				"                \"summary\": \"登录\",\n" +
				"                \"operationId\": \"loginUsingPOST\",\n" +
				"                \"consumes\": [\n" +
				"                    \"application/json\"\n" +
				"                ],\n" +
				"                \"produces\": [\n" +
				"                    \"*/*\"\n" +
				"                ],\n" +
				"                \"parameters\": [\n" +
				"                    {\n" +
				"                        \"in\": \"body\",\n" +
				"                        \"name\": \"requestVO\",\n" +
				"                        \"description\": \"requestVO\",\n" +
				"                        \"required\": true,\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/LoginRequestVO\"\n" +
				"                        }\n" +
				"                    }\n" +
				"                ],\n" +
				"                \"responses\": {\n" +
				"                    \"200\": {\n" +
				"                        \"description\": \"OK\",\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/ApiResult«登录响应对象VO»\"\n" +
				"                        }\n" +
				"                    },\n" +
				"                    \"201\": {\n" +
				"                        \"description\": \"Created\"\n" +
				"                    },\n" +
				"                    \"401\": {\n" +
				"                        \"description\": \"Unauthorized\"\n" +
				"                    },\n" +
				"                    \"403\": {\n" +
				"                        \"description\": \"Forbidden\"\n" +
				"                    },\n" +
				"                    \"404\": {\n" +
				"                        \"description\": \"Not Found\"\n" +
				"                    }\n" +
				"                },\n" +
				"                \"deprecated\": false\n" +
				"            }\n" +
				"        },\n" +
				"        \"/basic/getAuthCodeImg\": {\n" +
				"            \"post\": {\n" +
				"                \"tags\": [\n" +
				"                    \"基础工具Controller\"\n" +
				"                ],\n" +
				"                \"summary\": \"获取图片验证码\",\n" +
				"                \"operationId\": \"getAuthCodeImgUsingPOST\",\n" +
				"                \"consumes\": [\n" +
				"                    \"application/json\"\n" +
				"                ],\n" +
				"                \"produces\": [\n" +
				"                    \"*/*\"\n" +
				"                ],\n" +
				"                \"parameters\": [\n" +
				"                    {\n" +
				"                        \"in\": \"body\",\n" +
				"                        \"name\": \"requestVO\",\n" +
				"                        \"description\": \"requestVO\",\n" +
				"                        \"required\": true,\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/获取图片验证码请求对象VO\"\n" +
				"                        }\n" +
				"                    }\n" +
				"                ],\n" +
				"                \"responses\": {\n" +
				"                    \"200\": {\n" +
				"                        \"description\": \"OK\",\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/ApiResult«获取图片验证码响应对象VO»\"\n" +
				"                        }\n" +
				"                    },\n" +
				"                    \"201\": {\n" +
				"                        \"description\": \"Created\"\n" +
				"                    },\n" +
				"                    \"401\": {\n" +
				"                        \"description\": \"Unauthorized\"\n" +
				"                    },\n" +
				"                    \"403\": {\n" +
				"                        \"description\": \"Forbidden\"\n" +
				"                    },\n" +
				"                    \"404\": {\n" +
				"                        \"description\": \"Not Found\"\n" +
				"                    }\n" +
				"                },\n" +
				"                \"deprecated\": false\n" +
				"            }\n" +
				"        }\n" +
				"    },\n" +
				"    \"definitions\": {\n" +
				"        \"LoginRequestVO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"required\": [\n" +
				"                \"account\",\n" +
				"                \"identityType\",\n" +
				"                \"loginType\"\n" +
				"            ],\n" +
				"            \"properties\": {\n" +
				"                \"account\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"用户账号\"\n" +
				"                },\n" +
				"                \"code\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"验证码\"\n" +
				"                },\n" +
				"                \"countryCode\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"国家代码\"\n" +
				"                },\n" +
				"                \"identityType\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"身份类型 请参考UserServiceIdentityTypeEnum\"\n" +
				"                },\n" +
				"                \"loginType\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"登录方式  手机号 0 邮箱 1, 账号密码 2\"\n" +
				"                },\n" +
				"                \"password\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"密码\"\n" +
				"                },\n" +
				"                \"requestSource\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"请求来源\"\n" +
				"                },\n" +
				"                \"source\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"业务来源\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"LoginRequestVO\"\n" +
				"        },\n" +
				"        \"ApiResult«登录响应对象VO»\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"code\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"data\": {\n" +
				"                    \"$ref\": \"#/definitions/登录响应对象VO\"\n" +
				"                },\n" +
				"                \"msg\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"success\": {\n" +
				"                    \"type\": \"boolean\"\n" +
				"                },\n" +
				"                \"traceId\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"ApiResult«登录响应对象VO»\"\n" +
				"        },\n" +
				"        \"登录响应对象VO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"loginInfo\": {\n" +
				"                    \"description\": \"登录信息\",\n" +
				"                    \"$ref\": \"#/definitions/登录信息\"\n" +
				"                },\n" +
				"                \"token\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"token\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"登录响应对象VO\"\n" +
				"        },\n" +
				"        \"获取图片验证码请求对象VO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"source\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"业务来源\"\n" +
				"                },\n" +
				"                \"tenantId\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int64\",\n" +
				"                    \"description\": \"租户ID\"\n" +
				"                },\n" +
				"                \"testFlag\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"测试标记\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"获取图片验证码请求对象VO\"\n" +
				"        },\n" +
				"        \"ApiResult«获取图片验证码响应对象VO»\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"code\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"data\": {\n" +
				"                    \"$ref\": \"#/definitions/获取图片验证码响应对象VO\"\n" +
				"                },\n" +
				"                \"msg\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"success\": {\n" +
				"                    \"type\": \"boolean\"\n" +
				"                },\n" +
				"                \"traceId\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"ApiResult«获取图片验证码响应对象VO»\"\n" +
				"        },\n" +
				"        \"获取图片验证码响应对象VO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"authCodeImgBase64\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"图形验证码base64编码\"\n" +
				"                },\n" +
				"                \"authCodeKey\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"图形验证码key\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"获取图片验证码响应对象VO\"\n" +
				"        }\n" +
				"    }\n" +
				"}";

		// 使用McpRestTools创建服务器
		McpServer.SyncSpecification serverSpec = McpRestTools.createSyncServerFromSwagger(transportProvider,
				swaggerJson, "http://api.chongqing-test.business.xy/", "user-service", "1.0.0");

		// 为了测试添加一个模拟响应处理器 - 正常情况下这不需要，因为McpRestTools已经添加了处理器
		serverSpec.instructions("Test API for integration testing");

		// 构建并启动服务器
		McpSyncServer server = serverSpec.build();

		try {
			// 创建客户端并初始化
			try (var client = clientBuilder.clientInfo(new McpSchema.Implementation("Test Client", "1.0.0")).build()) {

				// 测试初始化
				InitializeResult initResult = client.initialize();
				assertThat(initResult).isNotNull();
				assertThat(initResult.serverInfo().name()).isEqualTo("user-service");
				assertThat(initResult.serverInfo().version()).isEqualTo("1.0.0");

				// 获取工具列表
				List<Tool> tools = client.listTools().tools();
				assertThat(tools).isNotEmpty();

				// 验证是否包含从Swagger生成的listPets工具
				Tool listPetsTool = findToolByName(tools, "getAuthCodeImgUsingPOST");
				assertThat(listPetsTool).isNotNull();
				assertThat(listPetsTool.description()).contains("获取图片验证码");
			}
		}
		finally {
			// 关闭服务器
			server.close();
		}
	}

	@Test
	public void testCallToolFromSyncServer() {
		// 创建简单的Swagger JSON - 这是一个简化的Petstore API定义
		String swaggerJson = "{\n" +
				"    \"swagger\": \"2.0\",\n" +
				"    \"info\": {\n" +
				"        \"description\": \"Api Documentation\",\n" +
				"        \"version\": \"1.0\",\n" +
				"        \"title\": \"Api Documentation\",\n" +
				"        \"termsOfService\": \"urn:tos\",\n" +
				"        \"contact\": {},\n" +
				"        \"license\": {\n" +
				"            \"name\": \"Apache 2.0\",\n" +
				"            \"url\": \"http://www.apache.org/licenses/LICENSE-2.0\"\n" +
				"        }\n" +
				"    },\n" +
				"    \"host\": \"api.chongqing-test.business.xy\",\n" +
				"    \"basePath\": \"/user-service\",\n" +
				"    \"tags\": [\n" +
				"        {\n" +
				"            \"name\": \"账号Controller\",\n" +
				"            \"description\": \"Account Controller\"\n" +
				"        }\n" +
				"    ],\n" +
				"    \"paths\": {\n" +
				"        \"/account/login\": {\n" +
				"            \"post\": {\n" +
				"                \"tags\": [\n" +
				"                    \"账号Controller\"\n" +
				"                ],\n" +
				"                \"summary\": \"登录\",\n" +
				"                \"operationId\": \"loginUsingPOST\",\n" +
				"                \"consumes\": [\n" +
				"                    \"application/json\"\n" +
				"                ],\n" +
				"                \"produces\": [\n" +
				"                    \"*/*\"\n" +
				"                ],\n" +
				"                \"parameters\": [\n" +
				"                    {\n" +
				"                        \"in\": \"body\",\n" +
				"                        \"name\": \"requestVO\",\n" +
				"                        \"description\": \"requestVO\",\n" +
				"                        \"required\": true,\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/LoginRequestVO\"\n" +
				"                        }\n" +
				"                    }\n" +
				"                ],\n" +
				"                \"responses\": {\n" +
				"                    \"200\": {\n" +
				"                        \"description\": \"OK\",\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/ApiResult«登录响应对象VO»\"\n" +
				"                        }\n" +
				"                    },\n" +
				"                    \"201\": {\n" +
				"                        \"description\": \"Created\"\n" +
				"                    },\n" +
				"                    \"401\": {\n" +
				"                        \"description\": \"Unauthorized\"\n" +
				"                    },\n" +
				"                    \"403\": {\n" +
				"                        \"description\": \"Forbidden\"\n" +
				"                    },\n" +
				"                    \"404\": {\n" +
				"                        \"description\": \"Not Found\"\n" +
				"                    }\n" +
				"                },\n" +
				"                \"deprecated\": false\n" +
				"            }\n" +
				"        },\n" +
				"        \"/basic/getAuthCodeImg\": {\n" +
				"            \"post\": {\n" +
				"                \"tags\": [\n" +
				"                    \"基础工具Controller\"\n" +
				"                ],\n" +
				"                \"summary\": \"获取图片验证码\",\n" +
				"                \"operationId\": \"getAuthCodeImgUsingPOST\",\n" +
				"                \"consumes\": [\n" +
				"                    \"application/json\"\n" +
				"                ],\n" +
				"                \"produces\": [\n" +
				"                    \"*/*\"\n" +
				"                ],\n" +
				"                \"parameters\": [\n" +
				"                    {\n" +
				"                        \"in\": \"body\",\n" +
				"                        \"name\": \"requestVO\",\n" +
				"                        \"description\": \"requestVO\",\n" +
				"                        \"required\": true,\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/获取图片验证码请求对象VO\"\n" +
				"                        }\n" +
				"                    }\n" +
				"                ],\n" +
				"                \"responses\": {\n" +
				"                    \"200\": {\n" +
				"                        \"description\": \"OK\",\n" +
				"                        \"schema\": {\n" +
				"                            \"$ref\": \"#/definitions/ApiResult«获取图片验证码响应对象VO»\"\n" +
				"                        }\n" +
				"                    },\n" +
				"                    \"201\": {\n" +
				"                        \"description\": \"Created\"\n" +
				"                    },\n" +
				"                    \"401\": {\n" +
				"                        \"description\": \"Unauthorized\"\n" +
				"                    },\n" +
				"                    \"403\": {\n" +
				"                        \"description\": \"Forbidden\"\n" +
				"                    },\n" +
				"                    \"404\": {\n" +
				"                        \"description\": \"Not Found\"\n" +
				"                    }\n" +
				"                },\n" +
				"                \"deprecated\": false\n" +
				"            }\n" +
				"        }\n" +
				"    },\n" +
				"    \"definitions\": {\n" +
				"        \"LoginRequestVO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"required\": [\n" +
				"                \"account\",\n" +
				"                \"identityType\",\n" +
				"                \"loginType\"\n" +
				"            ],\n" +
				"            \"properties\": {\n" +
				"                \"account\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"用户账号\"\n" +
				"                },\n" +
				"                \"code\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"验证码\"\n" +
				"                },\n" +
				"                \"countryCode\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"国家代码\"\n" +
				"                },\n" +
				"                \"identityType\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"身份类型 请参考UserServiceIdentityTypeEnum\"\n" +
				"                },\n" +
				"                \"loginType\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"登录方式  手机号 0 邮箱 1, 账号密码 2\"\n" +
				"                },\n" +
				"                \"password\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"密码\"\n" +
				"                },\n" +
				"                \"requestSource\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"请求来源\"\n" +
				"                },\n" +
				"                \"source\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"业务来源\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"LoginRequestVO\"\n" +
				"        },\n" +
				"        \"ApiResult«登录响应对象VO»\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"code\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"data\": {\n" +
				"                    \"$ref\": \"#/definitions/登录响应对象VO\"\n" +
				"                },\n" +
				"                \"msg\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"success\": {\n" +
				"                    \"type\": \"boolean\"\n" +
				"                },\n" +
				"                \"traceId\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"ApiResult«登录响应对象VO»\"\n" +
				"        },\n" +
				"        \"登录响应对象VO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"loginInfo\": {\n" +
				"                    \"description\": \"登录信息\",\n" +
				"                    \"$ref\": \"#/definitions/登录信息\"\n" +
				"                },\n" +
				"                \"token\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"token\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"登录响应对象VO\"\n" +
				"        },\n" +
				"        \"获取图片验证码请求对象VO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"source\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"业务来源\"\n" +
				"                },\n" +
				"                \"tenantId\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int64\",\n" +
				"                    \"description\": \"租户ID\"\n" +
				"                },\n" +
				"                \"testFlag\": {\n" +
				"                    \"type\": \"integer\",\n" +
				"                    \"format\": \"int32\",\n" +
				"                    \"description\": \"测试标记\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"获取图片验证码请求对象VO\"\n" +
				"        },\n" +
				"        \"ApiResult«获取图片验证码响应对象VO»\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"code\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"data\": {\n" +
				"                    \"$ref\": \"#/definitions/获取图片验证码响应对象VO\"\n" +
				"                },\n" +
				"                \"msg\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                },\n" +
				"                \"success\": {\n" +
				"                    \"type\": \"boolean\"\n" +
				"                },\n" +
				"                \"traceId\": {\n" +
				"                    \"type\": \"string\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"ApiResult«获取图片验证码响应对象VO»\"\n" +
				"        },\n" +
				"        \"获取图片验证码响应对象VO\": {\n" +
				"            \"type\": \"object\",\n" +
				"            \"properties\": {\n" +
				"                \"authCodeImgBase64\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"图形验证码base64编码\"\n" +
				"                },\n" +
				"                \"authCodeKey\": {\n" +
				"                    \"type\": \"string\",\n" +
				"                    \"description\": \"图形验证码key\"\n" +
				"                }\n" +
				"            },\n" +
				"            \"title\": \"获取图片验证码响应对象VO\"\n" +
				"        }\n" +
				"    }\n" +
				"}";

		// 使用McpRestTools创建服务器，并设置模拟响应处理器
		McpServer.SyncSpecification serverSpec = McpRestTools.createSyncServerFromSwagger(transportProvider,
				swaggerJson, "http://api.chongqing-test.business.xy/user-service", "user-service", "1.0.0");

		serverSpec.instructions("API for testing");

		// 构建并启动服务器
		McpSyncServer server = serverSpec.build();

		try {
			// 创建客户端并初始化
			try (var client = clientBuilder.clientInfo(new McpSchema.Implementation("user-service", "1.0.0")).build()) {

				// 初始化客户端
				InitializeResult initResult = client.initialize();
				assertThat(initResult).isNotNull();

				List<Tool> tools = client.listTools().tools();
				assertThat(tools).isNotEmpty();

				// 准备登录参数
				Map<String, Object> loginParams = Map.of("body", Map.of("source", "104", "tenantId", "1"));

				// 调用登录工具
				CallToolRequest loginRequest = new CallToolRequest("getAuthCodeImgUsingPOST", loginParams);

				// 执行工具调用
				CallToolResult result = client.callTool(loginRequest);

				// 验证响应
				assertThat(result).isNotNull();
				assertThat(result.isError()).isFalse();
				assertThat(result.content()).isNotEmpty();

				// 验证内容类型是TextContent
				assertThat(result.content().get(0)).isInstanceOf(TextContent.class);

				// 获取响应文本
				String responseText = ((TextContent) result.content().get(0)).text();
				assertThat(responseText).isNotNull();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Test failed", e);
		}
		finally {
			// 关闭服务器
			server.close();
		}
	}

	// 工具方法：通过名称查找工具
	private Tool findToolByName(List<Tool> tools, String name) {
		return tools.stream().filter(tool -> tool.name().equals(name)).findFirst().orElse(null);
	}

}