package com.wmatech.java_db_mcp.mongo.tool;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import com.wmatech.java_db_mcp.mongo.MongoPermissionService;
import org.bson.Document;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "mongo")
public class MongoInsertTool {

    private final MongoDatabase mongoDatabase;
    private final MongoPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public MongoInsertTool(MongoDatabase mongoDatabase, MongoPermissionService permissionService, ObjectMapper objectMapper) {
        this.mongoDatabase = mongoDatabase;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "mongo_insert", description = "Insert one or more documents into a MongoDB collection. Accepts a single JSON object or a JSON array.")
    public String insert(
            @McpToolParam(description = "Collection name", required = true) String collection,
            @McpToolParam(description = "Document(s) to insert (JSON object or JSON array)", required = true) String documents) {
        try {
            permissionService.requireWrite();
            long start = System.currentTimeMillis();
            List<Document> docs = parseDocuments(documents);
            MongoCollection<Document> coll = mongoDatabase.getCollection(collection);
            if (docs.size() == 1) {
                coll.insertOne(docs.get(0));
            } else {
                coll.insertMany(docs);
            }
            long elapsed = System.currentTimeMillis() - start;
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "insertedCount", docs.size(),
                    "executionTimeMs", elapsed
            ));
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing insert: " + e.getMessage();
        }
    }

    private List<Document> parseDocuments(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        List<Document> docs = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode element : node) {
                docs.add(Document.parse(element.toString()));
            }
        } else {
            docs.add(Document.parse(node.toString()));
        }
        return docs;
    }
}
