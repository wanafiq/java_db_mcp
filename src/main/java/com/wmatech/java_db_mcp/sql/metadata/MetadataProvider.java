package com.wmatech.java_db_mcp.sql.metadata;

import java.util.List;
import java.util.Map;

public interface MetadataProvider {

    List<Map<String, Object>> listTables();

    List<Map<String, Object>> describeTable(String tableName);
}
