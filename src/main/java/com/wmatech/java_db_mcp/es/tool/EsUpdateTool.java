package com.wmatech.java_db_mcp.es.tool;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import com.wmatech.java_db_mcp.es.EsPermissionService;
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
public class EsUpdateTool {

    private final EsRestClient client;
    private final EsPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public EsUpdateTool(EsRestClient client, EsPermissionService permissionService, ObjectMapper objectMapper) {
        this.client = client;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "es_update", description = "Partially update a document by ID. Pass the fields to set as `doc` JSON.")
    public String update(
            @McpToolParam(description = "Index name", required = true) String index,
            @McpToolParam(description = "Document ID", required = true) String id,
            @McpToolParam(description = "Partial fields JSON to merge (e.g. {\"status\":\"done\"})", required = true) String doc) {
        try {
            permissionService.requireWrite();
            long start = System.currentTimeMillis();
            ObjectNode payload = objectMapper.createObjectNode();
            payload.set("doc", objectMapper.readTree(doc));
            String response = client.request("POST",
                    "/" + EsRestClient.enc(index) + "/_update/" + EsRestClient.enc(id),
                    payload.toString());
            long elapsed = System.currentTimeMillis() - start;
            JsonNode tree = objectMapper.readTree(response);
            ObjectNode wrapper = tree.isObject() ? (ObjectNode) tree : objectMapper.createObjectNode().set("result", tree);
            wrapper.put("executionTimeMs", elapsed);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing update: " + e.getMessage();
        }
    }
}
