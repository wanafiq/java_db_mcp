package com.wmatech.java_db_mcp.es.tool;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import com.wmatech.java_db_mcp.es.EsPermissionService;
import com.wmatech.java_db_mcp.es.EsRestClient;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "elasticsearch")
public class EsIndexTool {

    private final EsRestClient client;
    private final EsPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public EsIndexTool(EsRestClient client, EsPermissionService permissionService, ObjectMapper objectMapper) {
        this.client = client;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "es_index", description = "Index (create/replace) a document. If id is omitted, Elasticsearch auto-generates one.")
    public String index(
            @McpToolParam(description = "Index name", required = true) String index,
            @McpToolParam(description = "Document JSON", required = true) String document,
            @McpToolParam(description = "Optional document ID", required = false) String id) {
        try {
            permissionService.requireWrite();
            long start = System.currentTimeMillis();
            String path = (id == null || id.isEmpty())
                    ? "/" + EsRestClient.enc(index) + "/_doc"
                    : "/" + EsRestClient.enc(index) + "/_doc/" + EsRestClient.enc(id);
            String method = (id == null || id.isEmpty()) ? "POST" : "PUT";
            String response = client.request(method, path, document);
            long elapsed = System.currentTimeMillis() - start;
            ObjectNode wrapper = (ObjectNode) objectMapper.readTree(response);
            wrapper.put("executionTimeMs", elapsed);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing index: " + e.getMessage();
        }
    }
}
