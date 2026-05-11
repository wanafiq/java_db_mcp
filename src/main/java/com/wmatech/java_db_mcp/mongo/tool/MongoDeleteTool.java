package com.wmatech.java_db_mcp.mongo.tool;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import com.wmatech.java_db_mcp.mongo.MongoPermissionService;
import org.bson.Document;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "mongo")
public class MongoDeleteTool {

    private final MongoDatabase mongoDatabase;
    private final MongoPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public MongoDeleteTool(MongoDatabase mongoDatabase, MongoPermissionService permissionService, ObjectMapper objectMapper) {
        this.mongoDatabase = mongoDatabase;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "mongo_delete", description = "Delete documents in a MongoDB collection.")
    public String delete(
            @McpToolParam(description = "Collection name", required = true) String collection,
            @McpToolParam(description = "Filter (extended JSON)", required = true) String filter,
            @McpToolParam(description = "Delete all matching documents (default false = deleteOne)", required = false) Boolean multi) {
        try {
            permissionService.requireDelete();
            long start = System.currentTimeMillis();
            Document filterDoc = Document.parse(filter);
            MongoCollection<Document> coll = mongoDatabase.getCollection(collection);
            DeleteResult result = (multi != null && multi) ? coll.deleteMany(filterDoc) : coll.deleteOne(filterDoc);
            long elapsed = System.currentTimeMillis() - start;
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "deletedCount", result.getDeletedCount(),
                    "executionTimeMs", elapsed
            ));
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing delete: " + e.getMessage();
        }
    }
}
