package com.wmatech.java_db_mcp.es.tool;

import com.wmatech.java_db_mcp.es.EsRestClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "elasticsearch")
public class EsGetTool {

    private final EsRestClient client;
    private final ObjectMapper objectMapper;

    public EsGetTool(EsRestClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "es_get", description = "Fetch a single document by ID from an Elasticsearch index.")
    public String get(
            @McpToolParam(description = "Index name", required = true) String index,
            @McpToolParam(description = "Document ID", required = true) String id) {
        try {
            long start = System.currentTimeMillis();
            String response = client.request("GET",
                    "/" + EsRestClient.enc(index) + "/_doc/" + EsRestClient.enc(id), null);
            long elapsed = System.currentTimeMillis() - start;
            ObjectNode wrapper = (ObjectNode) objectMapper.readTree(response);
            wrapper.put("executionTimeMs", elapsed);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (Exception e) {
            return "Error executing get: " + e.getMessage();
        }
    }
}
