/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.controller;

import com.mcp.rest.adapter.app.request.CreateServerRequest;
import com.mcp.rest.adapter.app.response.ServerInfo;
import com.mcp.rest.adapter.app.service.McpEmbeddedService;
import com.mcp.rest.adapter.app.servlet.DynamicServletInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * MCP嵌入式服务控制器
 * 所有MCP服务器共享同一个端点，通过serverId参数区分
 * 实际请求处理由DynamicServletInitializer的路由Servlet完成
 */
@RestController
@RequestMapping("/api/embeddedMcp")
public class McpEmbeddedController {

    private static final Logger logger = LoggerFactory.getLogger(McpEmbeddedController.class);

    // 端点常量
    private static final String SSE_ENDPOINT = DynamicServletInitializer.SSE_ENDPOINT;
    private static final String MESSAGE_ENDPOINT = DynamicServletInitializer.MESSAGE_ENDPOINT;

    @Autowired
    private McpEmbeddedService mcpEmbeddedService;

    /**
     * 创建新的MCP服务器
     */
    @PostMapping("/create")
    public ResponseEntity<ServerInfo> createServer(@RequestBody CreateServerRequest request) {
        try {
            ServerInfo serverInfo = mcpEmbeddedService.createServerFromSwagger(
                    request.getSwaggerJson(),
                    request.getBaseUrl(),
                    request.getServerName(),
                    request.getServerVersion());
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            logger.error("创建服务器失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取所有运行中的服务器
     */
    @PostMapping("/queryAll")
    public ResponseEntity<Map<String, ServerInfo>> getAllServers() {
        return ResponseEntity.ok(mcpEmbeddedService.getRunningServers());
    }

    /**
     * 停止指定的服务器
     */
    @PostMapping("/stop")
    public ResponseEntity<Void> stopServer(@RequestParam(value = "serverId") String serverId) {
        boolean success = mcpEmbeddedService.stopServer(serverId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
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
     * 测试调用MCP工具
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
     * 处理SSE连接请求 - 旧格式
     * 重定向到新的端点格式
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void handleSseConnection(
            @RequestParam(value = "serverId") String serverId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        logger.info("收到旧格式的SSE连接请求，将重定向到新端点");
        // 重定向到新的端点
        response.sendRedirect(SSE_ENDPOINT + "/" + serverId);
    }
    
    /**
     * 处理消息请求 - 旧格式
     * 重定向到新的端点格式
     */
    @RequestMapping(value = "/message", method = {RequestMethod.POST, RequestMethod.GET})
    public void handleMessage(
            @RequestParam(value = "serverId") String serverId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        logger.info("收到旧格式的消息请求，将重定向到新端点");
        // 重定向到新的端点
        if (request.getMethod().equals("GET")) {
            response.sendRedirect(MESSAGE_ENDPOINT + "/" + serverId);
        } else {
            // 对于POST请求，我们不能简单地重定向，需要转发
            response.sendError(HttpServletResponse.SC_MOVED_PERMANENTLY, 
                "请使用新的端点格式: " + MESSAGE_ENDPOINT + "/" + serverId);
        }
    }
    
    /**
     * 处理基于会话ID的消息请求
     * 现在已不再需要，由DynamicServletInitializer的路由Servlet处理
     */
    @RequestMapping(value = "/messageBySession", method = {RequestMethod.POST, RequestMethod.GET})
    public void handleMessageBySession(
            @RequestParam(value = "sessionId") String sessionId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        logger.info("收到基于会话ID的消息请求，此API已不再支持");
        response.sendError(HttpServletResponse.SC_GONE, "此API已不再支持，请使用新的端点格式");
    }
} 