package com.wmatech.java_db_mcp.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "mongo")
public class MongoClientConfig {

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(
            @Value("${DB_URI:}") String uri,
            @Value("${DB_HOST:localhost}") String host,
            @Value("${DB_PORT:27017}") int port,
            @Value("${DB_USER:}") String user,
            @Value("${DB_PASSWORD:}") String password,
            @Value("${DB_NAME:}") String name) {
        return MongoClients.create(buildConnectionString(uri, host, port, user, password, name));
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient client, @Value("${DB_NAME:}") String name) {
        if (name.isEmpty()) {
            throw new IllegalStateException("DB_NAME must be set when DB_TYPE=mongo");
        }
        return client.getDatabase(name);
    }

    private String buildConnectionString(String uri, String host, int port, String user, String password, String name) {
        if (!uri.isEmpty()) {
            return uri;
        }
        StringBuilder sb = new StringBuilder("mongodb://");
        if (!user.isEmpty()) {
            sb.append(user);
            if (!password.isEmpty()) {
                sb.append(':').append(password);
            }
            sb.append('@');
        }
        sb.append(host).append(':').append(port);
        if (!name.isEmpty()) {
            sb.append('/').append(name);
        }
        return sb.toString();
    }
}
