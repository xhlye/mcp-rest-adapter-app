# MCP REST API 服务器应用

这个应用程序提供了一个Web服务，允许你通过传入Swagger/OpenAPI文档来动态创建MCP服务器。

## 功能特点

- 通过REST API创建MCP服务器
- 从Swagger/OpenAPI文档自动生成MCP工具
- 动态管理多个MCP服务器实例
- 支持两种部署模式：
  - 独立Tomcat实例（每个服务器使用独立的端口和上下文路径）
  - 嵌入式服务器（所有服务器共享同一端点）

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

#### 创建MCP服务器（独立Tomcat模式）

```http
POST /api/mcp/create
Content-Type: application/json

{
  "swaggerJson": "你的Swagger JSON文档",
  "baseUrl": "http://你的API基础URL",
  "serverName": "服务名称",
  "serverVersion": "服务版本"
}
```

响应:

```json
{
  "id": "server-12345678",
  "url": "http://localhost:8081/mcp/sse",
  "name": "服务名称",
  "version": "服务版本"
}
```

#### 创建MCP服务器（嵌入式模式）

```http
POST /api/embeddedMcp/create
Content-Type: application/json

{
  "swaggerJson": "你的Swagger JSON文档",
  "baseUrl": "http://你的API基础URL",
  "serverName": "服务名称",
  "serverVersion": "服务版本"
}
```

响应:

```json
{
  "id": "server-12345678",
  "url": "http://localhost:8080/api/embeddedMcp/sse?serverId=server-12345678",
  "name": "服务名称",
  "version": "服务版本"
}
```

#### 获取所有服务器列表（独立模式）

```http
POST /api/mcp/queryAll
```

#### 获取所有服务器列表（嵌入式模式）

```http
POST /api/embeddedMcp/queryAll
```

#### 获取单个服务器信息（独立模式）

```http
POST /api/mcp/findById
Content-Type: application/x-www-form-urlencoded

serverId=server-12345678
```

#### 获取单个服务器信息（嵌入式模式）

```http
POST /api/embeddedMcp/findById
Content-Type: application/x-www-form-urlencoded

serverId=server-12345678
```

#### 停止服务器（独立模式）

```http
POST /api/mcp/stop
Content-Type: application/x-www-form-urlencoded

serverId=server-12345678
```

#### 停止服务器（嵌入式模式）

```http
POST /api/embeddedMcp/stop
Content-Type: application/x-www-form-urlencoded

serverId=server-12345678
```

#### 停止所有服务器（独立模式）

```http
POST /api/mcp/stopAll
```

#### 停止所有服务器（嵌入式模式）

```http
POST /api/embeddedMcp/stopAll
```

## 调用MCP服务器

创建服务器后，你可以使用返回的URL访问MCP服务器。

### 独立Tomcat模式下的MCP服务器端点：

- SSE端点: `{url}/sse`
- 消息端点: `{url}/message`

### 嵌入式模式下的MCP服务器端点：

- SSE端点: `/api/embeddedMcp/sse?serverId={serverId}`
- 消息端点: `/api/embeddedMcp/message?serverId={serverId}`

## 示例代码

### 使用cURL创建独立服务器

```bash
curl -X POST http://localhost:8080/api/mcp/create \
  -H "Content-Type: application/json" \
  -d '{
    "swaggerJson": "{你的Swagger JSON}",
    "baseUrl": "http://api.example.com",
    "serverName": "my-api",
    "serverVersion": "1.0.0"
  }'
```

### 使用cURL创建嵌入式服务器

```bash
curl -X POST http://localhost:8080/api/embeddedMcp/create \
  -H "Content-Type: application/json" \
  -d '{
    "swaggerJson": "{你的Swagger JSON}",
    "baseUrl": "http://api.example.com",
    "serverName": "my-api",
    "serverVersion": "1.0.0"
  }'
```

### 使用Java客户端连接独立MCP服务器

```java
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;

// 假设服务器URL为 http://localhost:8081/mcp/sse
String serverUrl = "http://localhost:8081/mcp";

// 创建客户端传输
var transport = new HttpClientSseClientTransport(
        serverUrl + "/sse",
        serverUrl + "/message"
);

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
    var result = client.callTool("toolName", Map.of("param1", "value1"));
    System.out.println("Result: " + result.getTextContent());
}
```

### 使用Java客户端连接嵌入式MCP服务器

```java
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;

// 假设服务器ID为 server-12345678
String serverId = "server-12345678";
String baseUrl = "http://localhost:8080";

// 创建客户端传输 - 使用builder模式，通过URL参数传递serverId
String sseUrl = baseUrl + "/api/embeddedMcp/sse?serverId=" + serverId;
var transport = HttpClientSseClientTransport.builder(sseUrl)
        .build();

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