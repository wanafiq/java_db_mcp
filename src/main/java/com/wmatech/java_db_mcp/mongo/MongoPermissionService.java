package com.wmatech.java_db_mcp.mongo;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "mongo")
public class MongoPermissionService {

    private final MongoPermissionProperties permissions;

    public MongoPermissionService(MongoPermissionProperties permissions) {
        this.permissions = permissions;
    }

    public void requireWrite() {
        if (!permissions.allowWrite()) {
            throw new PermissionDeniedException("WRITE operations are not allowed");
        }
    }

    public void requireDelete() {
        if (!permissions.allowDelete()) {
            throw new PermissionDeniedException("DELETE operations are not allowed");
        }
    }

    public void requireAdmin() {
        if (!permissions.allowAdmin()) {
            throw new PermissionDeniedException("ADMIN operations are not allowed");
        }
    }

    public String describePermissions() {
        var sb = new StringBuilder("Allowed operations: READ");
        if (permissions.allowWrite()) sb.append(", WRITE");
        if (permissions.allowDelete()) sb.append(", DELETE");
        if (permissions.allowAdmin()) sb.append(", ADMIN");
        return sb.toString();
    }
}
