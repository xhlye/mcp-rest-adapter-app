package com.mcp.rest.adapter.app.auth;

import com.mcp.rest.adapter.app.config.McpServerConfig;
import com.mcp.rest.adapter.app.servlet.DynamicServletInitializer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * API Key鉴权实现
 */
public class ApiKeyAuthenticator implements McpAuthenticator {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticator.class);
    
    // API Key通常放在请求头中
    private static final String API_KEY_HEADER = "X-API-KEY";
    
    // 服务ID与配置的映射
    private final Map<String, McpServerConfig> serverConfigs;
    
    public ApiKeyAuthenticator(Map<String, McpServerConfig> serverConfigs) {
        this.serverConfigs = serverConfigs;
    }
    
    @Override
    public boolean authenticate(HttpServletRequest request, String serverId) {
        // 获取服务配置
        McpServerConfig config = serverConfigs.get(serverId);
        if (config == null) {
            logger.warn("找不到服务配置: {}", serverId);
            return false;
        }
        
        // 如果服务未启用鉴权，直接通过
        if (config.getAuthType() == McpServerConfig.AuthType.NONE) {
            return true;
        }
        
        // 如果不是API Key鉴权类型，返回失败
        if (config.getAuthType() != McpServerConfig.AuthType.API_KEY) {
            logger.warn("服务 {} 不支持API Key鉴权", serverId);
            return false;
        }
        
        // 从请求中获取API Key
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("请求缺少API Key头: {}", API_KEY_HEADER);
            return false;
        }
        
        // 解析API Key (格式: keyId:secretKey)
        String[] parts = apiKey.split(":", 2);
        if (parts.length != 2) {
            logger.warn("API Key格式错误");
            return false;
        }
        
        String keyId = parts[0];
        String secretKey = parts[1];
        
        // 验证API Key
        Map<String, String> validApiKeys = config.getApiKeys();
        if (!validApiKeys.containsKey(keyId)) {
            logger.warn("无效的API Key ID: {}", keyId);
            return false;
        }
        
        String expectedSecret = validApiKeys.get(keyId);
        if (!expectedSecret.equals(secretKey)) {
            logger.warn("API Key密钥不匹配");
            return false;
        }
        
        logger.info("API Key鉴权成功: {}", keyId);
        return true;
    }
    
    @Override
    public String extractIdentity(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isEmpty()) {
            return "unknown";
        }
        
        String[] parts = apiKey.split(":", 2);
        if (parts.length != 2) {
            return "invalid";
        }
        
        return parts[0]; // 返回keyId作为身份标识
    }
} 