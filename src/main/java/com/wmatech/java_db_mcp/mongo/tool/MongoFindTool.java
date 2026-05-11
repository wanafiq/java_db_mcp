package com.wmatech.java_db_mcp.mongo.tool;

import com.mongodb.client.FindIterable;
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
public class MongoFindTool {

    private final MongoDatabase mongoDatabase;
    private final ObjectMapper objectMapper;
    private final PaginationProperties pagination;

    public MongoFindTool(MongoDatabase mongoDatabase, ObjectMapper objectMapper,
                         PaginationProperties pagination) {
        this.mongoDatabase = mongoDatabase;
        this.objectMapper = objectMapper;
        this.pagination = pagination;
    }

    @McpTool(name = "mongo_find", description = "Query documents in a MongoDB collection. filter/projection/sort are MongoDB extended JSON.")
    public String find(
            @McpToolParam(description = "Collection name", required = true) String collection,
            @McpToolParam(description = "Query filter (extended JSON, e.g. {\"status\":\"active\"})", required = false) String filter,
            @McpToolParam(description = "Projection (extended JSON, e.g. {\"name\":1,\"_id\":0})", required = false) String projection,
            @McpToolParam(description = "Sort (extended JSON, e.g. {\"createdAt\":-1})", required = false) String sort,
            @McpToolParam(description = "Max results (default 100, capped at MAX_ROWS)", required = false) Integer limit,
            @McpToolParam(description = "Skip first N results for pagination (default 0)", required = false) Integer skip) {
        try {
            long start = System.currentTimeMillis();
            int requested = limit == null ? 100 : limit;
            int effectiveLimit = Math.min(requested, pagination.maxRows());
            int skipN = skip == null ? 0 : Math.max(0, skip);

            FindIterable<Document> query = mongoDatabase.getCollection(collection).find(parseOrEmpty(filter));
            if (projection != null && !projection.isEmpty()) query = query.projection(Document.parse(projection));
            if (sort != null && !sort.isEmpty()) query = query.sort(Document.parse(sort));
            if (skipN > 0) query = query.skip(skipN);
            query = query.limit(effectiveLimit + 1);

            List<JsonNode> rows = new ArrayList<>();
            boolean truncated;
            try (MongoCursor<Document> cursor = query.cursor()) {
                while (cursor.hasNext() && rows.size() < effectiveLimit) {
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
            return "Error executing find: " + e.getMessage();
        }
    }

    private Document parseOrEmpty(String json) {
        return (json == null || json.isEmpty()) ? new Document() : Document.parse(json);
    }
}
