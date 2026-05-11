package com.wmatech.java_db_mcp.sql;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.sql.permissions")
public record SqlPermissionProperties(
        boolean allowInsert,
        boolean allowUpdate,
        boolean allowDelete,
        boolean allowDdl
) {
}
