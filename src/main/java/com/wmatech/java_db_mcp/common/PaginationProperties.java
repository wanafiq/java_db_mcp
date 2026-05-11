package com.wmatech.java_db_mcp.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.pagination")
public record PaginationProperties(
        int maxRows
) {
}
