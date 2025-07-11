package com.mcp.rest.adapter.app.controller;

import com.mcp.rest.adapter.app.request.CreateServerRequest;
import com.mcp.rest.adapter.app.response.ServerInfo;
import com.mcp.rest.adapter.app.service.McpLazyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 延迟启动MCP服务的控制器
 * 支持先创建服务配置，后续按需启动服务实例
 */
@RestController
@RequestMapping("/api/lazyMcp")
public class McpLazyController {

    private static final Logger logger = LoggerFactory.getLogger(McpLazyController.class);
    
    @Autowired
    private McpLazyService mcpLazyService;
    
    /**
     * 创建MCP服务配置（不启动服务）
     */
    @PostMapping("/create")
    public ResponseEntity<ServerInfo> create(@RequestBody CreateServerRequest request) {
        logger.info("接收到创建MCP服务配置请求，API名称: {}", request.getServerName());
        
        try {
            ServerInfo serverInfo = mcpLazyService.createServerConfig(
                    request.getSwaggerJson(),
                    request.getBaseUrl(),
                    request.getServerName(),
                    request.getServerVersion()
            );
            
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            logger.error("创建MCP服务配置失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 启动指定的MCP服务
     */
    @PostMapping("/start")
    public ResponseEntity<ServerInfo> startServer(@RequestParam(value = "serverId") String serverId) {
        logger.info("接收到启动MCP服务请求，服务ID: {}", serverId);
        
        try {
            ServerInfo serverInfo = mcpLazyService.startServer(serverId);
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            logger.error("启动MCP服务失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    
    /**
     * 获取所有服务配置（包括未启动和已启动的）
     */
    @PostMapping("/queryAll")
    public ResponseEntity<List<ServerInfo>> queryAll() {
        Map<String, ServerInfo> servers = mcpLazyService.getAllServers();
        List<ServerInfo> serverList = servers.values().stream().collect(Collectors.toList());
        return ResponseEntity.ok(serverList);
    }
    
    /**
     * 获取所有运行中的服务
     */
    @PostMapping("/queryRunning")
    public ResponseEntity<List<ServerInfo>> queryRunning() {
        Map<String, ServerInfo> servers = mcpLazyService.getRunningServers();
        List<ServerInfo> serverList = servers.values().stream().collect(Collectors.toList());
        return ResponseEntity.ok(serverList);
    }
    
    /**
     * 获取指定服务信息
     */
    @PostMapping("/findById")
    public ResponseEntity<ServerInfo> findById(@RequestParam(value = "serverId") String serverId) {
        ServerInfo serverInfo = mcpLazyService.getServerInfo(serverId);
        
        if (serverInfo == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(serverInfo);
    }
    
    /**
     * 停止服务
     */
    @PostMapping("/stop")
    public ResponseEntity<Void> stopServer(@RequestParam(value = "serverId") String serverId) {
        boolean success = mcpLazyService.stopServer(serverId);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 停止所有运行中的服务
     */
    @PostMapping("/stopAll")
    public ResponseEntity<Void> stopAllServers() {
        mcpLazyService.stopAllServers();
        return ResponseEntity.ok().build();
    }

    /**
     * 移除指定服务
     */
    @PostMapping("/remove")
    public ResponseEntity<Void> removeServer(@RequestParam(value = "serverId") String serverId) {
        logger.info("接收到移除MCP服务请求，服务ID: {}", serverId);
        
        boolean success = mcpLazyService.removeServer(serverId);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 移除所有服务
     */
    @PostMapping("/removeAll")
    public ResponseEntity<Void> removeAllServers() {
        logger.info("接收到移除所有MCP服务请求");
        mcpLazyService.removeAllServers();
        return ResponseEntity.ok().build();
    }
} 