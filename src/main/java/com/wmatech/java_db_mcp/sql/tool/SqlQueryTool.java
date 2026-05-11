package com.wmatech.java_db_mcp.sql.tool;

import com.wmatech.java_db_mcp.common.JsonFormatter;
import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import com.wmatech.java_db_mcp.sql.PermissionService;
import com.wmatech.java_db_mcp.sql.QueryService;
import com.wmatech.java_db_mcp.sql.QueryService.QueryResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnExpression("'${mcp.db.type:mysql}' == 'mysql' || '${mcp.db.type:mysql}' == 'postgres'")
public class SqlQueryTool {

    private final QueryService queryService;
    private final PermissionService permissionService;
    private final JsonFormatter jsonFormatter;

    public SqlQueryTool(QueryService queryService, PermissionService permissionService, JsonFormatter jsonFormatter) {
        this.queryService = queryService;
        this.permissionService = permissionService;
        this.jsonFormatter = jsonFormatter;
    }

    @McpTool(name = "sql_query", description = "Execute a SQL query against the configured SQL database. SELECT is always allowed. Other operations depend on server configuration.")
    public String query(
            @McpToolParam(description = "The SQL query to execute", required = true) String sql) {
        try {
            QueryResult result = queryService.execute(sql);
            return jsonFormatter.toJson(Map.of(
                    "rows", result.rows(),
                    "rowCount", result.rowCount(),
                    "executionTimeMs", result.executionTimeMs(),
                    "truncated", result.truncated()
            ));
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing query: " + e.getMessage();
        }
    }
}
