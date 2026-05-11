package com.wmatech.java_db_mcp.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "redis")
public class RedisClientConfig {

    @Bean(destroyMethod = "close")
    public JedisPooled jedisPooled(
            @Value("${DB_HOST:localhost}") String host,
            @Value("${DB_PORT:6379}") int port,
            @Value("${DB_USER:}") String user,
            @Value("${DB_PASSWORD:}") String password,
            @Value("${REDIS_DB:0}") int database,
            @Value("${REDIS_SSL:false}") boolean ssl,
            @Value("${REDIS_SSL_INSECURE:false}") boolean sslInsecure) throws Exception {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .user(user.isEmpty() ? null : user)
                .password(password.isEmpty() ? null : password)
                .database(database)
                .ssl(ssl);
        if (ssl && sslInsecure) {
            builder.sslSocketFactory(trustAllSslContext().getSocketFactory());
            builder.hostnameVerifier((h, s) -> true);
        }
        return new JedisPooled(new HostAndPort(host, port), builder.build());
    }

    private static SSLContext trustAllSslContext() throws Exception {
        TrustManager[] trustAll = {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new SecureRandom());
        return ctx;
    }
}
