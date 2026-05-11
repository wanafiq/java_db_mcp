package com.wmatech.java_db_mcp.redis;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "redis")
public class RedisPermissionService {

    private final RedisPermissionProperties permissions;

    public RedisPermissionService(RedisPermissionProperties permissions) {
        this.permissions = permissions;
    }

    public void require(RedisVerb verb) {
        switch (verb) {
            case READ -> {}
            case WRITE -> {
                if (!permissions.allowWrite()) {
                    throw new PermissionDeniedException("WRITE commands are not allowed");
                }
            }
            case ADMIN -> {
                if (!permissions.allowAdmin()) {
                    throw new PermissionDeniedException("ADMIN commands are not allowed");
                }
            }
        }
    }

    public String describePermissions() {
        var sb = new StringBuilder("Allowed verbs: READ");
        if (permissions.allowWrite()) sb.append(", WRITE");
        if (permissions.allowAdmin()) sb.append(", ADMIN");
        return sb.toString();
    }
}
