package com.wmatech.java_db_mcp.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;

public class AutoconfigExclusions implements EnvironmentPostProcessor {

    private static final List<String> JDBC_AUTOCONFIG = List.of(
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbType = environment.getProperty("DB_TYPE", "mysql").toLowerCase();
        if ("mysql".equals(dbType) || "postgres".equals(dbType)) {
            return;
        }
        String existing = environment.getProperty("spring.autoconfigure.exclude", "");
        String joined = String.join(",", JDBC_AUTOCONFIG);
        String combined = existing.isEmpty() ? joined : existing + "," + joined;
        environment.getPropertySources().addFirst(new MapPropertySource(
                "javaDbMcpAutoconfigExclusions",
                Map.of("spring.autoconfigure.exclude", combined)
        ));
    }
}
