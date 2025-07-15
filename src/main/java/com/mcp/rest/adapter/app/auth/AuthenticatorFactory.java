package com.mcp.rest.adapter.app.auth;

import com.mcp.rest.adapter.app.config.McpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 鉴权工厂类，根据配置创建对应的鉴权器
 */
public class AuthenticatorFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticatorFactory.class);
    
    /**
     * 创建鉴权器
     * @param serverConfigs 服务ID与配置的映射
     * @return 鉴权器实例
     */
    public static McpAuthenticator createAuthenticator(Map<String, McpServerConfig> serverConfigs) {
        // 检查是否有任何服务配置了API Key鉴权
        boolean hasApiKeyAuth = serverConfigs.values().stream()
                .anyMatch(config -> config.getAuthType() == McpServerConfig.AuthType.API_KEY);
        
        // 检查是否有任何服务配置了JWT Token鉴权
        boolean hasJwtAuth = serverConfigs.values().stream()
                .anyMatch(config -> config.getAuthType() == McpServerConfig.AuthType.JWT_TOKEN);
        
        // 检查是否有任何服务配置了自定义鉴权
        boolean hasCustomAuth = serverConfigs.values().stream()
                .anyMatch(config -> config.getAuthType() == McpServerConfig.AuthType.CUSTOM);
        
        // 根据配置创建鉴权器
        if (hasCustomAuth) {
            // 如果有自定义鉴权，尝试创建自定义鉴权器
            try {
                // 获取第一个配置了自定义鉴权的服务配置
                McpServerConfig customConfig = serverConfigs.values().stream()
                        .filter(config -> config.getAuthType() == McpServerConfig.AuthType.CUSTOM)
                        .findFirst()
                        .orElseThrow();
                
                // 加载自定义鉴权类
                String className = customConfig.getCustomAuthClass();
                Class<?> clazz = Class.forName(className);
                
                // 检查是否实现了McpAuthenticator接口
                if (!McpAuthenticator.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("自定义鉴权类必须实现McpAuthenticator接口");
                }
                
                // 创建实例
                return (McpAuthenticator) clazz.getDeclaredConstructor(Map.class).newInstance(serverConfigs);
            } catch (Exception e) {
                logger.error("创建自定义鉴权器失败", e);
                // 回退到API Key或JWT鉴权
            }
        }
        
        if (hasJwtAuth) {
            logger.info("创建JWT Token鉴权器");
            return new JwtAuthenticator(serverConfigs);
        }
        
        if (hasApiKeyAuth) {
            logger.info("创建API Key鉴权器");
            return new ApiKeyAuthenticator(serverConfigs);
        }
        
        // 默认返回一个总是通过的鉴权器
        logger.info("创建默认鉴权器（无鉴权）");
        return new McpAuthenticator() {
            @Override
            public boolean authenticate(jakarta.servlet.http.HttpServletRequest request, String serverId) {
                return true;
            }
            
            @Override
            public String extractIdentity(jakarta.servlet.http.HttpServletRequest request) {
                return "anonymous";
            }
        };
    }
} 