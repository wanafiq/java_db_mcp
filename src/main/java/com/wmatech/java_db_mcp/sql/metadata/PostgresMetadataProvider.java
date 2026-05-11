package com.wmatech.java_db_mcp.sql.metadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "postgres")
public class PostgresMetadataProvider implements MetadataProvider {

    private final JdbcTemplate jdbcTemplate;

    public PostgresMetadataProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> listTables() {
        return jdbcTemplate.queryForList("""
                SELECT
                    n.nspname AS "schemaName",
                    c.relname AS "tableName",
                    c.reltuples::bigint AS "estimatedRowCount",
                    pg_relation_size(c.oid) AS "dataSizeBytes",
                    pg_indexes_size(c.oid) AS "indexSizeBytes",
                    NULL::timestamp AS "createTime",
                    NULL::timestamp AS "updateTime"
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE c.relkind = 'r'
                  AND n.nspname NOT IN ('pg_catalog', 'information_schema')
                  AND n.nspname NOT LIKE 'pg_toast%'
                  AND n.nspname NOT LIKE 'pg_temp_%'
                ORDER BY n.nspname, c.relname
                """);
    }

    @Override
    public List<Map<String, Object>> describeTable(String tableName) {
        String[] parts = tableName.split("\\.", 2);
        String schemaParam = parts.length == 2 ? parts[0] : null;
        String tableParam = parts.length == 2 ? parts[1] : tableName;

        return jdbcTemplate.queryForList("""
                SELECT
                    c.table_schema AS "schemaName",
                    c.column_name AS "columnName",
                    c.data_type AS "dataType",
                    CASE
                        WHEN c.character_maximum_length IS NOT NULL
                            THEN c.data_type || '(' || c.character_maximum_length || ')'
                        WHEN c.data_type IN ('numeric', 'decimal') AND c.numeric_precision IS NOT NULL
                            THEN c.data_type || '(' || c.numeric_precision || ',' || COALESCE(c.numeric_scale, 0) || ')'
                        ELSE c.data_type
                    END AS "columnType",
                    c.is_nullable AS "nullable",
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM information_schema.table_constraints tc
                            JOIN information_schema.key_column_usage kcu
                                ON tc.constraint_name = kcu.constraint_name
                                AND tc.table_schema = kcu.table_schema
                            WHERE tc.constraint_type = 'PRIMARY KEY'
                              AND kcu.table_schema = c.table_schema
                              AND kcu.table_name = c.table_name
                              AND kcu.column_name = c.column_name
                        ) THEN 'PRI'
                        ELSE ''
                    END AS "columnKey",
                    c.column_default AS "defaultValue",
                    CASE WHEN c.is_identity = 'YES' THEN 'identity' ELSE '' END AS "extra"
                FROM information_schema.columns c
                WHERE c.table_schema = COALESCE(?, current_schema())
                  AND c.table_name = ?
                ORDER BY c.ordinal_position
                """, schemaParam, tableParam);
    }
}
