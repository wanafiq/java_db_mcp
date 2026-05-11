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
public class EsDeleteTool {

    private final EsRestClient client;
    private final EsPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public EsDeleteTool(EsRestClient client, EsPermissionService permissionService, ObjectMapper objectMapper) {
        this.client = client;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "es_delete", description = "Delete a document by ID from an Elasticsearch index.")
    public String delete(
            @McpToolParam(description = "Index name", required = true) String index,
            @McpToolParam(description = "Document ID", required = true) String id) {
        try {
            permissionService.requireDelete();
            long start = System.currentTimeMillis();
            String response = client.request("DELETE",
                    "/" + EsRestClient.enc(index) + "/_doc/" + EsRestClient.enc(id), null);
            long elapsed = System.currentTimeMillis() - start;
            ObjectNode wrapper = (ObjectNode) objectMapper.readTree(response);
            wrapper.put("executionTimeMs", elapsed);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing delete: " + e.getMessage();
        }
    }
}
