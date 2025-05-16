/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.controller;

import io.modelcontextprotocol.app.rest.request.CreateServerRequest;
import io.modelcontextprotocol.app.rest.response.ServerInfo;
import io.modelcontextprotocol.app.rest.service.McpEmbeddedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处理MCP嵌入式服务器的控制器
 */
@RestController
@RequestMapping("/api/embeddedMcp")
public class McpEmbeddedController {

    private static final Logger logger = LoggerFactory.getLogger(McpEmbeddedController.class);
    
    @Autowired
    private McpEmbeddedService mcpEmbeddedService;
    
    /**
     * 创建MCP服务器
     */
    @PostMapping("/create")
    public ResponseEntity<ServerInfo> create(@RequestBody CreateServerRequest request) {
        logger.info("接收到创建MCP服务器请求，API名称: {}", request.getServerName());
        
        try {
            ServerInfo serverInfo = mcpEmbeddedService.createServerFromSwagger(
                    request.getSwaggerJson(),
                    request.getBaseUrl(),
                    request.getServerName(),
                    request.getServerVersion()
            );
            
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            logger.error("创建MCP服务器失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取所有服务器
     */
    @PostMapping("/queryAll")
    public ResponseEntity<List<ServerInfo>> queryAll() {
        Map<String, ServerInfo> servers = mcpEmbeddedService.getRunningServers();
        List<ServerInfo> serverList = servers.values().stream().collect(Collectors.toList());
        return ResponseEntity.ok(serverList);
    }
    
    /**
     * 获取单个服务器
     */
    @PostMapping("/findById")
    public ResponseEntity<ServerInfo> findById(@RequestParam(value = "serverId")  String serverId) {
        Map<String, ServerInfo> servers = mcpEmbeddedService.getRunningServers();
        ServerInfo serverInfo = servers.get(serverId);
        
        if (serverInfo == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(serverInfo);
    }
    
    /**
     * 停止服务器
     */
    @PostMapping("/stop")
    public ResponseEntity<Void> stopServer(@RequestParam(value = "serverId") String serverId) {
        boolean success = mcpEmbeddedService.stopServer(serverId);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 停止所有服务器
     */
    @PostMapping("/stopAll")
    public ResponseEntity<Void> stopAllServers() {
        mcpEmbeddedService.stopAllServers();
        return ResponseEntity.ok().build();
    }

    /**
     * 测试客户端调用
     */
    @PostMapping("/testCall")
    public ResponseEntity<String> testCall(
            @RequestParam(value = "serverId") String serverId,
            @RequestParam(value = "toolName") String toolName,
            @RequestBody Map<String, Object> params) {
        try {
            String result = mcpEmbeddedService.testCall(serverId, toolName, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("测试调用失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("测试调用失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理SSE连接 - 共享端点
     * 所有MCP服务器共用此端点，通过serverId参数区分不同实例
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void handleSseConnection(
            @RequestParam(value = "serverId", required = true) String serverId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        logger.info("接收到SSE连接请求，服务器ID: {}", serverId);
        mcpEmbeddedService.handleSseRequest(serverId, request, response);
    }
    
    /**
     * 处理消息请求 - 共享端点
     * 所有MCP服务器共用此端点，通过sessionId参数区分不同会话
     */
    @PostMapping("/message")
    public void handleMessage(
            @RequestParam(value = "sessionId", required = true) String sessionId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        logger.info("接收到消息请求，会话ID: {}, 请求类型: {}, 内容长度: {}", 
                    sessionId, request.getContentType(), request.getContentLength());
        
        try {
            // 日志记录请求内容类型和长度
            logger.debug("消息请求详情 - 类型: {}, 长度: {}, ContentType: {}, CharacterEncoding: {}",
                      request.getMethod(), request.getContentLength(), 
                      request.getContentType(), request.getCharacterEncoding());
            
            // 提前读取请求体并记录日志（只在调试级别）
            if (logger.isDebugEnabled()) {
                StringBuilder requestBody = new StringBuilder();
                String line;
                try (var reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                logger.debug("消息请求体内容: {}", requestBody.toString());
                
                // 创建一个新的请求包装器，使用预先读取的内容
                request = new RequestBodyCachingWrapper(request, requestBody.toString());
            }
            
            // 直接传递请求给服务处理
            mcpEmbeddedService.handleMessageRequestBySessionId(sessionId, request, response);
        } catch (Exception e) {
            logger.error("处理消息请求异常: {}", e.getMessage(), e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "处理消息请求失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 请求体缓存包装器，允许多次读取请求体
     */
    private static class RequestBodyCachingWrapper extends HttpServletRequestWrapper {
        private final String body;
        
        public RequestBodyCachingWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body;
        }
        
        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
            
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }
                
                @Override
                public boolean isReady() {
                    return true;
                }
                
                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }
                
                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };
        }
        
        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new StringReader(body));
        }
    }
} 