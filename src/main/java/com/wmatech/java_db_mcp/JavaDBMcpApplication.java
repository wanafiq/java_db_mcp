package com.wmatech.java_db_mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JavaDBMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaDBMcpApplication.class, args);
	}

}
