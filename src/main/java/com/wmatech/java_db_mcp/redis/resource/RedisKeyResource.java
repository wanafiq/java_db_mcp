package com.wmatech.java_db_mcp.redis.resource;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "redis")
public class RedisKeyResource {

    private final JedisPooled jedis;
    private final ObjectMapper objectMapper;

    public RedisKeyResource(JedisPooled jedis, ObjectMapper objectMapper) {
        this.jedis = jedis;
        this.objectMapper = objectMapper;
    }

    @McpResource(uri = "db://keys", name = "Keys",
            description = "Sample of keys (up to 100) via SCAN. For pattern matching, use redis_command(SCAN, [\"0\",\"MATCH\",\"...\",\"COUNT\",\"100\"]).")
    public String listKeys() {
        try {
            ScanResult<String> result = jedis.scan("0", new ScanParams().count(100));
            List<Map<String, Object>> entries = new ArrayList<>();
            for (String key : result.getResult()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("key", key);
                entry.put("type", jedis.type(key));
                entry.put("ttlSeconds", jedis.ttl(key));
                entries.add(entry);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entries);
        } catch (Exception e) {
            return "Error listing keys: " + e.getMessage();
        }
    }

    @McpResource(uri = "db://keys/{key}", name = "Key Value",
            description = "Returns the type, ttl, and decoded value of a specific key")
    public String getKey(String key) {
        try {
            String type = jedis.type(key);
            if ("none".equals(type)) {
                return "Key not found: " + key;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("key", key);
            result.put("type", type);
            result.put("ttlSeconds", jedis.ttl(key));
            result.put("value", decodeValue(key, type));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "Error reading key: " + e.getMessage();
        }
    }

    private Object decodeValue(String key, String type) {
        return switch (type) {
            case "string" -> jedis.get(key);
            case "list" -> jedis.lrange(key, 0, -1);
            case "hash" -> jedis.hgetAll(key);
            case "set" -> jedis.smembers(key);
            case "zset" -> {
                List<Tuple> tuples = jedis.zrangeWithScores(key, 0, -1);
                List<Map<String, Object>> zset = new ArrayList<>(tuples.size());
                for (Tuple t : tuples) {
                    zset.add(Map.of("member", t.getElement(), "score", t.getScore()));
                }
                yield zset;
            }
            case "stream" -> "(stream entries not displayed -- use redis_command(XRANGE, ...))";
            default -> null;
        };
    }
}
