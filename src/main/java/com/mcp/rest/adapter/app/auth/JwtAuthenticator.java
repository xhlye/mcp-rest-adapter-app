package com.mcp.rest.adapter.app.auth;

import com.mcp.rest.adapter.app.config.McpServerConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Map;

/**
 * JWT Token鉴权实现
 */
public class JwtAuthenticator implements McpAuthenticator {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticator.class);
    
    // JWT Token通常放在Authorization头中，格式为"Bearer {token}"
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    // 服务ID与配置的映射
    private final Map<String, McpServerConfig> serverConfigs;
    
    public JwtAuthenticator(Map<String, McpServerConfig> serverConfigs) {
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
        
        // 如果不是JWT Token鉴权类型，返回失败
        if (config.getAuthType() != McpServerConfig.AuthType.JWT_TOKEN) {
            logger.warn("服务 {} 不支持JWT Token鉴权", serverId);
            return false;
        }
        
        // 从请求中获取JWT Token
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("请求缺少有效的Authorization头");
            return false;
        }
        
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (token.isEmpty()) {
            logger.warn("JWT Token为空");
            return false;
        }
        
        try {
            // 简单验证JWT Token的格式
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.warn("JWT Token格式错误");
                return false;
            }
            
            // 解析JWT Payload (简化实现，实际应用中应使用专门的JWT库进行验证)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            // 在实际应用中，这里应该验证:
            // 1. 签名是否有效 (使用config.getJwtSecret())
            // 2. token是否过期
            // 3. 其他必要的声明(claims)是否存在且有效
            
            logger.info("JWT Token鉴权成功");
            return true;
        } catch (Exception e) {
            logger.error("JWT Token验证失败", e);
            return false;
        }
    }
    
    @Override
    public String extractIdentity(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return "unknown";
            }
            
            String token = authHeader.substring(BEARER_PREFIX.length());
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return "invalid";
            }
            
            // 从JWT Payload中提取subject作为身份标识
            // 实际应用中应使用专门的JWT库解析
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // 简化实现，实际中应该使用JSON解析库提取subject或其他身份标识
            if (payload.contains("\"sub\":")) {
                int start = payload.indexOf("\"sub\":") + 7;
                int end = payload.indexOf("\"", start);
                if (end > start) {
                    return payload.substring(start, end);
                }
            }
            
            return "jwt-user";
        } catch (Exception e) {
            logger.error("提取JWT身份失败", e);
            return "error";
        }
    }
} 