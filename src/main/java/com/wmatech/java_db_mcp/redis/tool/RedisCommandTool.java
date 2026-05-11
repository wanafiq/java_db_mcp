package com.wmatech.java_db_mcp.redis.tool;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import com.wmatech.java_db_mcp.redis.RedisCommandClassifier;
import com.wmatech.java_db_mcp.redis.RedisPermissionService;
import com.wmatech.java_db_mcp.redis.RedisVerb;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.commands.ProtocolCommand;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "redis")
public class RedisCommandTool {

    private final JedisPooled jedis;
    private final RedisCommandClassifier classifier;
    private final RedisPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public RedisCommandTool(JedisPooled jedis, RedisCommandClassifier classifier,
                            RedisPermissionService permissionService, ObjectMapper objectMapper) {
        this.jedis = jedis;
        this.classifier = classifier;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "redis_command", description = "Execute a Redis command. Reads always allowed; writes require REDIS_ALLOW_WRITE; admin commands require REDIS_ALLOW_ADMIN. Unknown commands are rejected.")
    public String execute(
            @McpToolParam(description = "Redis command name (e.g. GET, SET, LRANGE)", required = true) String command,
            @McpToolParam(description = "Arguments as a JSON array of strings (e.g. [\"mykey\",\"myvalue\"])", required = false) String args) {
        try {
            RedisVerb verb = classifier.classify(command);
            permissionService.require(verb);
            String[] argArray = parseArgs(args);
            long start = System.currentTimeMillis();
            Object result = jedis.sendCommand(toCommand(command), argArray);
            long elapsed = System.currentTimeMillis() - start;
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "result", normalize(result),
                    "executionTimeMs", elapsed
            ));
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    private String[] parseArgs(String json) throws Exception {
        if (json == null || json.isEmpty()) return new String[0];
        JsonNode arr = objectMapper.readTree(json);
        if (!arr.isArray()) throw new IllegalArgumentException("args must be a JSON array");
        String[] result = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++) result[i] = arr.get(i).asString();
        return result;
    }

    private static ProtocolCommand toCommand(String cmd) {
        String upper = cmd.toUpperCase();
        try {
            return Protocol.Command.valueOf(upper);
        } catch (IllegalArgumentException e) {
            byte[] raw = upper.getBytes(StandardCharsets.UTF_8);
            return () -> raw;
        }
    }

    private Object normalize(Object result) {
        if (result == null) return null;
        if (result instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
        if (result instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) out.add(normalize(item));
            return out;
        }
        return result;
    }
}
