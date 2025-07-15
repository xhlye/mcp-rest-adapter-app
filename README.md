# MCP REST API 服务器应用

这个应用程序提供了一个Web服务，允许你通过传入Swagger/OpenAPI文档来动态创建MCP服务器。通过该应用，你可以将任何REST API转换为MCP工具，并通过MCP协议提供给客户端使用。

## 功能特点

- 通过REST API动态创建MCP服务器
- 从Swagger/OpenAPI文档自动生成MCP工具
- 动态管理多个MCP服务器实例
- 支持嵌入式模式部署（所有服务器共享同一端点）
- 提供鉴权和限流功能

## 使用方法

### 构建和运行

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar target/mcp-rest-adapter-app-1.0.0-SNAPSHOT.jar
```

或者直接使用Maven运行：

```bash
mvn spring-boot:run
```

默认情况下，服务器会在8080端口启动。

### API接口

#### 创建MCP服务器

```http
POST /api/dynamicMcp/create
Content-Type: application/json

{
  "serverInfo": {
    "swaggerJson": "你的Swagger JSON文档",
    "baseUrl": "http://你的API基础URL",
    "serverName": "服务名称",
    "serverVersion": "服务版本",
    "headers": {
      "自定义请求头名称": "值"
    }
  },
  "apiAuth": {
    "bearerToken": "Bearer令牌",
    "username": "用户名",
    "password": "密码",
    "apiKey": "API密钥",
    "apiKeyName": "API密钥名称",
    "apiKeyLocation": "API密钥位置",
    "customAuthToken": "自定义认证令牌"
  },
  "mcpAuth": {
    "mcpAuthType": "NONE",
    "mcpApiKeys": {
      "clientId": "secretKey"
    },
    "mcpJwtSecret": "JWT密钥",
    "mcpCustomAuthClass": "自定义认证类"
  },
  "performanceConfig": {
    "enableRateLimiting": false,
    "maxTps": 100,
    "maxConcurrentRequests": 50,
    "requestTimeoutMs": 30000
  }
}
```

> 注意：在`apiAuth`中，你可以选择一种认证方式（Bearer令牌、基本认证、API密钥或自定义认证）。在`mcpAuth`中，你可以选择MCP服务器的认证类型（无认证、API密钥、JWT或自定义认证）。

响应:

```json
{
  "id": "server-12345678",
  "url": "http://localhost:8080/mcp/server-12345678/sse",
  "name": "服务名称",
  "version": "服务版本"
}
```

#### 获取所有服务器列表

```http
GET /api/dynamicMcp/queryAll
```

响应:

```json
{
  "server-12345678": {
    "id": "server-12345678",
    "url": "http://localhost:8080/mcp/server-12345678/sse",
    "name": "服务名称",
    "version": "服务版本"
  },
  "server-87654321": {
    "id": "server-87654321",
    "url": "http://localhost:8080/mcp/server-87654321/sse",
    "name": "另一个服务",
    "version": "2.0.0"
  }
}
```

#### 停止指定的服务器

```http
POST /api/dynamicMcp/stop
Content-Type: application/x-www-form-urlencoded

serverId=server-12345678
```

#### 停止所有服务器

```http
POST /api/dynamicMcp/stopAll
```

## 调用MCP服务器

创建服务器后，你可以使用返回的URL访问MCP服务器。

### MCP服务器端点

- SSE端点: `/mcp/{serverId}/sse`
- 消息端点: `/mcp/{serverId}/message`

## 示例代码

### 使用cURL创建MCP服务器

```bash
curl -X POST http://localhost:8080/api/dynamicMcp/create \
  -H "Content-Type: application/json" \
  -d '{
    "serverInfo": {
      "swaggerJson": "{你的Swagger JSON}",
      "baseUrl": "http://api.example.com",
      "serverName": "my-api",
      "serverVersion": "1.0.0"
    }
  }'
```

### 使用Java客户端连接MCP服务器

```java
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;

// 假设服务器ID为 server-12345678
String serverId = "server-12345678";
String baseUrl = "http://localhost:8080";

// 创建客户端传输
String sseUrl = baseUrl + "/mcp/" + serverId + "/sse";
String messageUrl = baseUrl + "/mcp/" + serverId + "/message";
var transport = new HttpClientSseClientTransport(sseUrl, messageUrl);

// 创建并初始化客户端
try (var client = McpClient.sync(transport)
        .clientInfo(new McpSchema.Implementation("Test Client", "1.0.0"))
        .build()) {

    // 初始化连接
    var initResult = client.initialize();
    System.out.println("Connected to: " + initResult.serverInfo().name() + " v" + initResult.serverInfo().version());

    // 获取工具列表
    var tools = client.listTools().tools();
    System.out.println("Available tools: " + tools.size());

    // 调用工具
    var toolRequest = new McpSchema.CallToolRequest("toolName", Map.of("param1", "value1"));
    var result = client.callTool(toolRequest);
    
    // 获取响应文本
    if (!result.isError() && result.content().size() > 0) {
        String responseText = ((McpSchema.TextContent) result.content().get(0)).text();
        System.out.println("Result: " + responseText);
    }
}
```

## 高级配置

### 鉴权配置

MCP服务器支持以下鉴权方式：

1. **无鉴权** - 所有客户端都可以访问
2. **API密钥鉴权** - 客户端需要提供有效的API密钥
3. **JWT鉴权** - 客户端需要提供有效的JWT令牌
4. **自定义鉴权** - 使用自定义鉴权实现类

### 限流配置

可以为每个MCP服务器配置以下限流参数：

1. **最大TPS** - 每秒最大请求数
2. **最大并发请求数** - 同时处理的最大请求数
3. **请求超时** - 请求超时时间（毫秒）

## 技术架构

该应用基于以下技术构建：

- Spring Boot - Web框架
- MCP SDK - Model Context Protocol实现
- Swagger Parser - 解析OpenAPI规范
- Apache HttpClient - HTTP客户端
- Apache Tomcat - 嵌入式Web服务器 