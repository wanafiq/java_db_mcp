package com.wmatech.java_db_mcp.mongo.tool;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.wmatech.java_db_mcp.common.PaginationProperties;
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
public class MongoAggregateTool {

    private final MongoDatabase mongoDatabase;
    private final ObjectMapper objectMapper;
    private final PaginationProperties pagination;

    public MongoAggregateTool(MongoDatabase mongoDatabase, ObjectMapper objectMapper,
                              PaginationProperties pagination) {
        this.mongoDatabase = mongoDatabase;
        this.objectMapper = objectMapper;
        this.pagination = pagination;
    }

    @McpTool(name = "mongo_aggregate", description = "Run an aggregation pipeline on a MongoDB collection. Pipeline is a JSON array of stage objects. Results are capped at MAX_ROWS.")
    public String aggregate(
            @McpToolParam(description = "Collection name", required = true) String collection,
            @McpToolParam(description = "Pipeline (JSON array of stages, e.g. [{\"$match\":{...}},{\"$group\":{...}}])", required = true) String pipeline) {
        try {
            long start = System.currentTimeMillis();
            List<Document> stages = parsePipeline(pipeline);
            AggregateIterable<Document> result = mongoDatabase.getCollection(collection).aggregate(stages);
            int max = pagination.maxRows();

            List<JsonNode> rows = new ArrayList<>();
            boolean truncated;
            try (MongoCursor<Document> cursor = result.cursor()) {
                while (cursor.hasNext() && rows.size() < max) {
                    rows.add(objectMapper.readTree(cursor.next().toJson()));
                }
                truncated = cursor.hasNext();
            }
            long elapsed = System.currentTimeMillis() - start;
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "rows", rows,
                    "rowCount", rows.size(),
                    "executionTimeMs", elapsed,
                    "truncated", truncated
            ));
        } catch (Exception e) {
            return "Error executing aggregate: " + e.getMessage();
        }
    }

    private List<Document> parsePipeline(String json) throws Exception {
        JsonNode arr = objectMapper.readTree(json);
        if (!arr.isArray()) {
            throw new IllegalArgumentException("Pipeline must be a JSON array");
        }
        List<Document> stages = new ArrayList<>();
        for (JsonNode stage : arr) {
            stages.add(Document.parse(stage.toString()));
        }
        return stages;
    }
}
