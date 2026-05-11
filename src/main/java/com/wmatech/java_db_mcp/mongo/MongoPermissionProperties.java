package com.wmatech.java_db_mcp.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.mongo.permissions")
public record MongoPermissionProperties(
        boolean allowWrite,
        boolean allowDelete,
        boolean allowAdmin
) {
}
