package com.wmatech.java_db_mcp.es;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.es.permissions")
public record EsPermissionProperties(
        boolean allowWrite,
        boolean allowDelete,
        boolean allowAdmin
) {
}
