package com.wmatech.java_db_mcp.sql;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final SqlParser sqlParser;
    private final SqlPermissionProperties permissions;

    public PermissionService(SqlParser sqlParser, SqlPermissionProperties permissions) {
        this.sqlParser = sqlParser;
        this.permissions = permissions;
    }

    public QueryType validateAndGetType(String sql) {
        QueryType type = sqlParser.parse(sql);
        return switch (type) {
            case SELECT -> type;
            case INSERT -> {
                if (!permissions.allowInsert()) {
                    throw new PermissionDeniedException("INSERT operations are not allowed");
                }
                yield type;
            }
            case UPDATE -> {
                if (!permissions.allowUpdate()) {
                    throw new PermissionDeniedException("UPDATE operations are not allowed");
                }
                yield type;
            }
            case DELETE -> {
                if (!permissions.allowDelete()) {
                    throw new PermissionDeniedException("DELETE operations are not allowed");
                }
                yield type;
            }
            case DDL -> {
                if (!permissions.allowDdl()) {
                    throw new PermissionDeniedException("DDL operations are not allowed");
                }
                yield type;
            }
            case OTHER -> throw new PermissionDeniedException("Unsupported SQL statement type");
        };
    }

    public String describePermissions() {
        var sb = new StringBuilder("Allowed operations: SELECT");
        if (permissions.allowInsert()) sb.append(", INSERT");
        if (permissions.allowUpdate()) sb.append(", UPDATE");
        if (permissions.allowDelete()) sb.append(", DELETE");
        if (permissions.allowDdl()) sb.append(", DDL (CREATE/ALTER/DROP/TRUNCATE)");
        return sb.toString();
    }
}
