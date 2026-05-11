package com.wmatech.java_db_mcp.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.redis.permissions")
public record RedisPermissionProperties(
        boolean allowWrite,
        boolean allowAdmin
) {
}
