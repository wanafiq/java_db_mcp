package com.wmatech.java_db_mcp.mongo.tool;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
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
public class MongoUpdateTool {

    private final MongoDatabase mongoDatabase;
    private final MongoPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public MongoUpdateTool(MongoDatabase mongoDatabase, MongoPermissionService permissionService, ObjectMapper objectMapper) {
        this.mongoDatabase = mongoDatabase;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "mongo_update", description = "Update documents in a MongoDB collection. update should use operators like $set, $inc, $push.")
    public String update(
            @McpToolParam(description = "Collection name", required = true) String collection,
            @McpToolParam(description = "Filter (extended JSON)", required = true) String filter,
            @McpToolParam(description = "Update document (extended JSON, e.g. {\"$set\":{\"status\":\"done\"}})", required = true) String update,
            @McpToolParam(description = "Update all matching documents (default false = updateOne)", required = false) Boolean multi) {
        try {
            permissionService.requireWrite();
            long start = System.currentTimeMillis();
            Document filterDoc = Document.parse(filter);
            Document updateDoc = Document.parse(update);
            MongoCollection<Document> coll = mongoDatabase.getCollection(collection);
            UpdateResult result = (multi != null && multi)
                    ? coll.updateMany(filterDoc, updateDoc)
                    : coll.updateOne(filterDoc, updateDoc);
            long elapsed = System.currentTimeMillis() - start;
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "matchedCount", result.getMatchedCount(),
                    "modifiedCount", result.getModifiedCount(),
                    "executionTimeMs", elapsed
            ));
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing update: " + e.getMessage();
        }
    }
}
