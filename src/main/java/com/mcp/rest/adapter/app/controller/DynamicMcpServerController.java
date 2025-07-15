/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.controller;

import com.mcp.rest.adapter.app.request.CreateServerRequest;
import com.mcp.rest.adapter.app.response.ServerInfo;
import com.mcp.rest.adapter.app.service.DynamicMcpServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 动态MCP服务控制器
 * 所有MCP服务器共享同一个端点，通过serverId参数区分
 * 实际请求处理由DynamicServletInitializer的路由Servlet完成
 */
@RestController
@RequestMapping("/api/dynamicMcp")
public class DynamicMcpServerController {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMcpServerController.class);

    @Autowired
    private DynamicMcpServerService dynamicMcpServerService;

    /**
     * 创建MCP服务器
     */
    @PostMapping("/create")
    public ResponseEntity<ServerInfo> create(@RequestBody CreateServerRequest request, HttpServletRequest httpRequest) {
        logger.info("接收到创建MCP服务器请求: {}", request.getServerInfo().getServerName());
        
        try {
            ServerInfo serverInfo = dynamicMcpServerService.createServerFromSwagger(request, httpRequest);
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            logger.error("创建MCP服务器失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取所有服务器
     */
    @GetMapping("/queryAll")
    public ResponseEntity<Map<String, ServerInfo>> getAllServers(HttpServletRequest httpRequest) {
        try {
            Map<String, ServerInfo> servers = dynamicMcpServerService.getRunningServers(httpRequest);
            return ResponseEntity.ok(servers);
        } catch (Exception e) {
            logger.error("获取服务器列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 停止指定的服务器
     */
    @PostMapping("/stop")
    public ResponseEntity<Void> stopServer(@RequestParam(value = "serverId") String serverId) {
        boolean success = dynamicMcpServerService.stopServer(serverId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * 停止所有服务器
     */
    @PostMapping("/stopAll")
    public ResponseEntity<Void> stopAllServers() {
        dynamicMcpServerService.stopAllServers();
        return ResponseEntity.ok().build();
    }

} 