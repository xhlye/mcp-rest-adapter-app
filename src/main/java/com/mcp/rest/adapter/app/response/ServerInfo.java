/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.response;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * MCP服务器信息响应对象
 */
public class ServerInfo {

    private final String serverId;
    private final String serverUrl;
    private final String serverName;
    private final String serverVersion;
    private final List<McpSchema.Tool> tools;

    public ServerInfo(String serverId, String serverUrl, String serverName, String serverVersion) {
        this(serverId, serverUrl, serverName, serverVersion, null);
    }

    public ServerInfo(String serverId, String serverUrl, String serverName, String serverVersion, List<McpSchema.Tool> tools) {
        this.serverId = serverId;
        this.serverUrl = serverUrl;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.tools = tools;
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public List<McpSchema.Tool> getTools() {
        return tools;
    }
} 