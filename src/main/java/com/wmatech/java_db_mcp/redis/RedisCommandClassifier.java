package com.wmatech.java_db_mcp.redis;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.wmatech.java_db_mcp.redis.RedisVerb.ADMIN;
import static com.wmatech.java_db_mcp.redis.RedisVerb.READ;
import static com.wmatech.java_db_mcp.redis.RedisVerb.WRITE;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "redis")
public class RedisCommandClassifier {

    private static final Map<String, RedisVerb> COMMANDS = Map.<String, RedisVerb>ofEntries(
            // string
            Map.entry("GET", READ), Map.entry("MGET", READ), Map.entry("GETRANGE", READ), Map.entry("STRLEN", READ),
            Map.entry("SET", WRITE), Map.entry("SETEX", WRITE), Map.entry("PSETEX", WRITE), Map.entry("SETNX", WRITE),
            Map.entry("MSET", WRITE), Map.entry("MSETNX", WRITE), Map.entry("APPEND", WRITE), Map.entry("SETRANGE", WRITE),
            Map.entry("INCR", WRITE), Map.entry("DECR", WRITE), Map.entry("INCRBY", WRITE), Map.entry("DECRBY", WRITE),
            Map.entry("INCRBYFLOAT", WRITE), Map.entry("GETSET", WRITE), Map.entry("GETDEL", WRITE), Map.entry("GETEX", WRITE),
            // key
            Map.entry("EXISTS", READ), Map.entry("TYPE", READ), Map.entry("TTL", READ), Map.entry("PTTL", READ),
            Map.entry("KEYS", READ), Map.entry("SCAN", READ), Map.entry("RANDOMKEY", READ), Map.entry("DBSIZE", READ),
            Map.entry("OBJECT", READ), Map.entry("DUMP", READ),
            Map.entry("DEL", WRITE), Map.entry("UNLINK", WRITE), Map.entry("RENAME", WRITE), Map.entry("RENAMENX", WRITE),
            Map.entry("COPY", WRITE), Map.entry("EXPIRE", WRITE), Map.entry("PEXPIRE", WRITE), Map.entry("EXPIREAT", WRITE),
            Map.entry("PEXPIREAT", WRITE), Map.entry("PERSIST", WRITE), Map.entry("TOUCH", WRITE), Map.entry("MOVE", WRITE),
            Map.entry("RESTORE", WRITE),
            // hash
            Map.entry("HGET", READ), Map.entry("HMGET", READ), Map.entry("HKEYS", READ), Map.entry("HVALS", READ),
            Map.entry("HLEN", READ), Map.entry("HEXISTS", READ), Map.entry("HGETALL", READ), Map.entry("HSCAN", READ),
            Map.entry("HSTRLEN", READ), Map.entry("HRANDFIELD", READ),
            Map.entry("HSET", WRITE), Map.entry("HMSET", WRITE), Map.entry("HSETNX", WRITE), Map.entry("HDEL", WRITE),
            Map.entry("HINCRBY", WRITE), Map.entry("HINCRBYFLOAT", WRITE),
            // list
            Map.entry("LRANGE", READ), Map.entry("LLEN", READ), Map.entry("LINDEX", READ), Map.entry("LPOS", READ),
            Map.entry("LPUSH", WRITE), Map.entry("RPUSH", WRITE), Map.entry("LPUSHX", WRITE), Map.entry("RPUSHX", WRITE),
            Map.entry("LPOP", WRITE), Map.entry("RPOP", WRITE), Map.entry("BLPOP", WRITE), Map.entry("BRPOP", WRITE),
            Map.entry("LMPOP", WRITE), Map.entry("LREM", WRITE), Map.entry("LTRIM", WRITE), Map.entry("LSET", WRITE),
            Map.entry("LINSERT", WRITE), Map.entry("LMOVE", WRITE), Map.entry("RPOPLPUSH", WRITE),
            // set
            Map.entry("SMEMBERS", READ), Map.entry("SISMEMBER", READ), Map.entry("SMISMEMBER", READ), Map.entry("SCARD", READ),
            Map.entry("SRANDMEMBER", READ), Map.entry("SDIFF", READ), Map.entry("SUNION", READ), Map.entry("SINTER", READ),
            Map.entry("SINTERCARD", READ), Map.entry("SSCAN", READ),
            Map.entry("SADD", WRITE), Map.entry("SREM", WRITE), Map.entry("SMOVE", WRITE), Map.entry("SPOP", WRITE),
            Map.entry("SDIFFSTORE", WRITE), Map.entry("SUNIONSTORE", WRITE), Map.entry("SINTERSTORE", WRITE),
            // sorted set
            Map.entry("ZRANGE", READ), Map.entry("ZREVRANGE", READ), Map.entry("ZRANGEBYSCORE", READ),
            Map.entry("ZREVRANGEBYSCORE", READ), Map.entry("ZRANGEBYLEX", READ), Map.entry("ZREVRANGEBYLEX", READ),
            Map.entry("ZCARD", READ), Map.entry("ZCOUNT", READ), Map.entry("ZLEXCOUNT", READ), Map.entry("ZSCORE", READ),
            Map.entry("ZMSCORE", READ), Map.entry("ZRANK", READ), Map.entry("ZREVRANK", READ), Map.entry("ZSCAN", READ),
            Map.entry("ZRANDMEMBER", READ),
            Map.entry("ZADD", WRITE), Map.entry("ZREM", WRITE), Map.entry("ZINCRBY", WRITE),
            Map.entry("ZREMRANGEBYRANK", WRITE), Map.entry("ZREMRANGEBYSCORE", WRITE), Map.entry("ZREMRANGEBYLEX", WRITE),
            Map.entry("ZRANGESTORE", WRITE), Map.entry("ZUNIONSTORE", WRITE), Map.entry("ZINTERSTORE", WRITE),
            Map.entry("ZDIFFSTORE", WRITE), Map.entry("ZPOPMAX", WRITE), Map.entry("ZPOPMIN", WRITE),
            Map.entry("BZPOPMAX", WRITE), Map.entry("BZPOPMIN", WRITE),
            // stream
            Map.entry("XREAD", READ), Map.entry("XRANGE", READ), Map.entry("XREVRANGE", READ), Map.entry("XLEN", READ),
            Map.entry("XINFO", READ),
            Map.entry("XADD", WRITE), Map.entry("XDEL", WRITE), Map.entry("XTRIM", WRITE), Map.entry("XACK", WRITE),
            Map.entry("XCLAIM", WRITE), Map.entry("XAUTOCLAIM", WRITE), Map.entry("XGROUP", WRITE),
            Map.entry("XREADGROUP", WRITE), Map.entry("XSETID", WRITE),
            // bitmap
            Map.entry("GETBIT", READ), Map.entry("BITCOUNT", READ), Map.entry("BITPOS", READ), Map.entry("BITFIELD_RO", READ),
            Map.entry("SETBIT", WRITE), Map.entry("BITOP", WRITE), Map.entry("BITFIELD", WRITE),
            // geo
            Map.entry("GEOPOS", READ), Map.entry("GEODIST", READ), Map.entry("GEOHASH", READ), Map.entry("GEOSEARCH", READ),
            Map.entry("GEOADD", WRITE), Map.entry("GEOSEARCHSTORE", WRITE),
            // hyperloglog
            Map.entry("PFCOUNT", READ),
            Map.entry("PFADD", WRITE), Map.entry("PFMERGE", WRITE),
            // pub/sub
            Map.entry("PUBSUB", READ),
            Map.entry("PUBLISH", WRITE),
            // server / connection (read)
            Map.entry("PING", READ), Map.entry("ECHO", READ), Map.entry("INFO", READ), Map.entry("TIME", READ),
            Map.entry("CLIENT", READ), Map.entry("HELLO", READ), Map.entry("SELECT", READ), Map.entry("AUTH", READ),
            Map.entry("RESET", READ), Map.entry("SORT_RO", READ), Map.entry("WAIT", READ), Map.entry("DISCARD", READ),
            Map.entry("WATCH", READ), Map.entry("UNWATCH", READ),
            // sort / multi (write — SORT can write via STORE option)
            Map.entry("SORT", WRITE), Map.entry("MULTI", WRITE), Map.entry("EXEC", WRITE),
            // server / admin
            Map.entry("FLUSHDB", ADMIN), Map.entry("FLUSHALL", ADMIN), Map.entry("CONFIG", ADMIN),
            Map.entry("SHUTDOWN", ADMIN), Map.entry("DEBUG", ADMIN), Map.entry("MONITOR", ADMIN),
            Map.entry("BGSAVE", ADMIN), Map.entry("SAVE", ADMIN), Map.entry("BGREWRITEAOF", ADMIN),
            Map.entry("LASTSAVE", ADMIN), Map.entry("REPLICAOF", ADMIN), Map.entry("SLAVEOF", ADMIN),
            Map.entry("CLUSTER", ADMIN), Map.entry("ACL", ADMIN), Map.entry("SCRIPT", ADMIN),
            Map.entry("FUNCTION", ADMIN), Map.entry("LATENCY", ADMIN), Map.entry("MEMORY", ADMIN),
            Map.entry("FAILOVER", ADMIN), Map.entry("PSYNC", ADMIN), Map.entry("REPLCONF", ADMIN),
            // scripting (admin — arbitrary code execution)
            Map.entry("EVAL", ADMIN), Map.entry("EVALSHA", ADMIN), Map.entry("EVAL_RO", ADMIN),
            Map.entry("EVALSHA_RO", ADMIN), Map.entry("FCALL", ADMIN), Map.entry("FCALL_RO", ADMIN)
    );

    public RedisVerb classify(String command) {
        RedisVerb verb = COMMANDS.get(command.toUpperCase());
        if (verb == null) {
            throw new PermissionDeniedException("Unknown Redis command: " + command);
        }
        return verb;
    }
}
