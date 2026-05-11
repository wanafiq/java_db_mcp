package com.wmatech.java_db_mcp.mongo.resource;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "mcp.db.type", havingValue = "mongo")
public class MongoCollectionResource {

    private final MongoDatabase mongoDatabase;
    private final ObjectMapper objectMapper;

    public MongoCollectionResource(MongoDatabase mongoDatabase, ObjectMapper objectMapper) {
        this.mongoDatabase = mongoDatabase;
        this.objectMapper = objectMapper;
    }

    @McpResource(uri = "db://collections", name = "Collections",
            description = "Lists all collections in the database with estimated document count")
    public String listCollections() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (String name : mongoDatabase.listCollectionNames()) {
                MongoCollection<Document> coll = mongoDatabase.getCollection(name);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("collectionName", name);
                entry.put("estimatedDocumentCount", coll.estimatedDocumentCount());
                list.add(entry);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
        } catch (Exception e) {
            return "Error listing collections: " + e.getMessage();
        }
    }

    @McpResource(uri = "db://collections/{collectionName}", name = "Collection Detail",
            description = "Returns a sample document and the indexes for a specific collection")
    public String describeCollection(String collectionName) {
        try {
            MongoCollection<Document> coll = mongoDatabase.getCollection(collectionName);
            Document sample = coll.find().first();
            List<JsonNode> indexes = new ArrayList<>();
            for (Document idx : coll.listIndexes()) {
                indexes.add(objectMapper.readTree(idx.toJson()));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("collection", collectionName);
            result.put("sampleDocument", sample == null ? null : objectMapper.readTree(sample.toJson()));
            result.put("indexes", indexes);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return "Error describing collection: " + e.getMessage();
        }
    }
}
