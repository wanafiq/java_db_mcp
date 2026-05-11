package com.wmatech.java_db_mcp.es.resource;

import com.wmatech.java_db_mcp.es.EsRestClient;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "elasticsearch")
public class EsIndexResource {

    private final EsRestClient client;
    private final ObjectMapper objectMapper;

    public EsIndexResource(EsRestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @McpResource(uri = "db://indices", name = "Indices",
            description = "Lists all indices with doc count, store size, health, and status (via _cat/indices).")
    public String listIndices() {
        try {
            String response = client.request("GET", "/_cat/indices?format=json&bytes=b", null);
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(response));
        } catch (Exception e) {
            return "Error listing indices: " + e.getMessage();
        }
    }

    @McpResource(uri = "db://indices/{indexName}", name = "Index Detail",
            description = "Returns settings and mappings for a specific index.")
    public String describeIndex(String indexName) {
        try {
            String response = client.request("GET", "/" + EsRestClient.enc(indexName), null);
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(response));
        } catch (Exception e) {
            return "Error describing index: " + e.getMessage();
        }
    }
}
