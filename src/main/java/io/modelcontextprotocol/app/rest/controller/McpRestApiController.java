/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.controller;

import io.modelcontextprotocol.app.rest.request.CreateServerRequest;
import io.modelcontextprotocol.app.rest.response.ServerInfo;
import io.modelcontextprotocol.app.rest.service.McpRestApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST控制器，提供创建和管理MCP服务器的接口
 */
@RestController
@RequestMapping("/api/isolateMcp")
public class McpRestApiController {

    private static final Logger logger = LoggerFactory.getLogger(McpRestApiController.class);
    
    private final McpRestApiService mcpRestApiService;
    
    public McpRestApiController(McpRestApiService mcpRestApiService) {
        this.mcpRestApiService = mcpRestApiService;
    }

    /**
     * 创建MCP服务器
     */
    @PostMapping("/create")
    public ResponseEntity<ServerInfo> create(@RequestBody CreateServerRequest request) {
        logger.info("Received request to create MCP server for API: {}", request.getServerName());
        
        try {
            
            ServerInfo serverInfo = mcpRestApiService.createServerFromSwagger(
                    request.getSwaggerJson(),
                    request.getBaseUrl(),
                    request.getServerName(),
                    request.getServerVersion()
            );
            
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            logger.error("Failed to create MCP server", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取所有服务器
     */
    @PostMapping("/queryAll")
    public ResponseEntity<List<ServerInfo>> queryAll() {
        Map<String, ServerInfo> servers = mcpRestApiService.getRunningServers();
        List<ServerInfo> serverList = servers.values().stream().collect(Collectors.toList());
        return ResponseEntity.ok(serverList);
    }
    
    /**
     * 获取特定服务器
     */
    @PostMapping("/findById")
    public ResponseEntity<ServerInfo> findById(@RequestParam(value = "serverId") String serverId) {
        Map<String, ServerInfo> servers = mcpRestApiService.getRunningServers();
        ServerInfo serverInfo = servers.get(serverId);
        
        if (serverInfo == null) {
            return ResponseEntity.ok(null);
        }
        
        return ResponseEntity.ok(serverInfo);
    }
    
    /**
     * 停止服务器
     */
    @PostMapping("/stop")
    public ResponseEntity<Void> stopServer(@RequestParam(value = "serverId") String serverId) {
        boolean success = mcpRestApiService.stopServer(serverId);
        
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
        mcpRestApiService.stopAllServers();
        return ResponseEntity.ok().build();
    }
} 