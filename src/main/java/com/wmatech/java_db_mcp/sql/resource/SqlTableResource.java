package com.wmatech.java_db_mcp.sql.resource;

import com.wmatech.java_db_mcp.common.JsonFormatter;
import com.wmatech.java_db_mcp.sql.metadata.MetadataProvider;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("'${mcp.db.type:mysql}' == 'mysql' || '${mcp.db.type:mysql}' == 'postgres'")
public class SqlTableResource {

    private final MetadataProvider metadataProvider;
    private final JsonFormatter jsonFormatter;

    public SqlTableResource(MetadataProvider metadataProvider, JsonFormatter jsonFormatter) {
        this.metadataProvider = metadataProvider;
        this.jsonFormatter = jsonFormatter;
    }

    @McpResource(uri = "db://tables", name = "Tables",
            description = "Lists all tables in the database with metadata (row count, data size, index size, timestamps)")
    public String listTables() {
        return jsonFormatter.toJson(metadataProvider.listTables());
    }

    @McpResource(uri = "db://tables/{tableName}", name = "Table Schema",
            description = "Returns column details for a specific table (name, type, nullable, key, default, extra)")
    public String getTableSchema(String tableName) {
        List<Map<String, Object>> columns = metadataProvider.describeTable(tableName);
        if (columns.isEmpty()) {
            return "Table not found: " + tableName;
        }
        return jsonFormatter.toJson(columns);
    }
}
