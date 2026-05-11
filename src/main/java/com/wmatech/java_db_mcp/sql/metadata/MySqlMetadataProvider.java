package com.wmatech.java_db_mcp.sql.metadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "mysql", matchIfMissing = true)
public class MySqlMetadataProvider implements MetadataProvider {

    private final JdbcTemplate jdbcTemplate;

    public MySqlMetadataProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> listTables() {
        return jdbcTemplate.queryForList("""
                SELECT
                    TABLE_SCHEMA AS schemaName,
                    TABLE_NAME AS tableName,
                    TABLE_ROWS AS estimatedRowCount,
                    DATA_LENGTH AS dataSizeBytes,
                    INDEX_LENGTH AS indexSizeBytes,
                    CREATE_TIME AS createTime,
                    UPDATE_TIME AS updateTime
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                ORDER BY TABLE_NAME
                """);
    }

    @Override
    public List<Map<String, Object>> describeTable(String tableName) {
        return jdbcTemplate.queryForList("""
                SELECT
                    COLUMN_NAME AS columnName,
                    DATA_TYPE AS dataType,
                    COLUMN_TYPE AS columnType,
                    IS_NULLABLE AS nullable,
                    COLUMN_KEY AS columnKey,
                    COLUMN_DEFAULT AS defaultValue,
                    EXTRA AS extra
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """, tableName);
    }
}
