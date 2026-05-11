package com.wmatech.java_db_mcp.es.tool;

import com.wmatech.java_db_mcp.common.PaginationProperties;
import com.wmatech.java_db_mcp.es.EsRestClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "elasticsearch")
public class EsSearchTool {

    private final EsRestClient client;
    private final ObjectMapper objectMapper;
    private final PaginationProperties pagination;

    public EsSearchTool(EsRestClient client, ObjectMapper objectMapper, PaginationProperties pagination) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.pagination = pagination;
    }

    @McpTool(name = "es_search", description = "Run a search query against an Elasticsearch index using the Query DSL. `size` is capped at MAX_ROWS.")
    public String search(
            @McpToolParam(description = "Index name or alias", required = true) String index,
            @McpToolParam(description = "Search body (Query DSL as JSON, e.g. {\"query\":{\"match_all\":{}}})", required = false) String query,
            @McpToolParam(description = "Max results (default 10, capped at MAX_ROWS)", required = false) Integer size,
            @McpToolParam(description = "From offset for pagination (default 0)", required = false) Integer from) {
        try {
            long start = System.currentTimeMillis();
            ObjectNode payload = (query == null || query.isEmpty())
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(query);
            int requestedSize = size == null ? 10 : size;
            int effectiveSize = Math.min(requestedSize, pagination.maxRows());
            payload.put("size", effectiveSize);
            if (from != null) payload.put("from", from);
            String response = client.request("POST", "/" + EsRestClient.enc(index) + "/_search", payload.toString());
            long elapsed = System.currentTimeMillis() - start;
            JsonNode tree = objectMapper.readTree(response);
            ObjectNode wrapper = tree.isObject() ? (ObjectNode) tree : objectMapper.createObjectNode().set("result", tree);
            long totalHits = tree.path("hits").path("total").path("value").asLong(-1);
            int returnedHits = tree.path("hits").path("hits").size();
            boolean truncated = totalHits > 0 && totalHits > (from == null ? 0 : from) + returnedHits;
            wrapper.put("executionTimeMs", elapsed);
            wrapper.put("truncated", truncated);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (Exception e) {
            return "Error executing search: " + e.getMessage();
        }
    }
}
