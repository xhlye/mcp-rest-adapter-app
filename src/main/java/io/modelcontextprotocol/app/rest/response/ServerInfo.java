/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.response;

/**
 * MCP服务器信息，用于API响应
 */
public class ServerInfo {

    /**
     * 服务器唯一标识符，用于后续操作引用该服务器
     */
    private final String id;

    /**
     * 服务器访问URL，客户端通过此URL连接到MCP服务器
     */
    private final String url;

    /**
     * 服务器名称，用户指定的服务器标识名
     */
    private final String name;

    /**
     * 服务器版本号
     */
    private final String version;

    /**
     * 构造函数
     * @param id 服务器ID
     * @param url 服务器URL
     * @param name 服务器名称
     * @param version 服务器版本
     */
    public ServerInfo(String id, String url, String name, String version) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.version = version;
    }

    /**
     * 获取服务器ID
     * @return 服务器ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取服务器URL
     * @return 服务器URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * 获取服务器名称
     * @return 服务器名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取服务器版本
     * @return 服务器版本
     */
    public String getVersion() {
        return version;
    }
} 