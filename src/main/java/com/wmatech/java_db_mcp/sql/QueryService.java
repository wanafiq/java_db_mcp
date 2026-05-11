package com.wmatech.java_db_mcp.sql;

import com.wmatech.java_db_mcp.common.PaginationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnExpression("'${mcp.db.type:mysql}' == 'mysql' || '${mcp.db.type:mysql}' == 'postgres'")
public class QueryService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;
    private final PaginationProperties pagination;

    public QueryService(JdbcTemplate jdbcTemplate, PermissionService permissionService,
                        PaginationProperties pagination) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
        this.pagination = pagination;
    }

    public QueryResult execute(String sql) {
        QueryType type = permissionService.validateAndGetType(sql);
        long start = System.currentTimeMillis();

        if (type == QueryType.SELECT) {
            return executeReadQuery(sql, start);
        } else {
            return executeWriteQuery(sql, start);
        }
    }

    @Transactional(readOnly = true)
    protected QueryResult executeReadQuery(String sql, long start) {
        int max = pagination.maxRows();
        int requestLimit = max + 1;
        List<Map<String, Object>> rows = jdbcTemplate.query(
                conn -> {
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setMaxRows(requestLimit);
                    return ps;
                },
                new ColumnMapRowMapper()
        );
        boolean truncated = rows.size() > max;
        if (truncated) {
            rows = rows.subList(0, max);
        }
        long elapsed = System.currentTimeMillis() - start;
        return new QueryResult(rows, rows.size(), elapsed, truncated);
    }

    @Transactional
    protected QueryResult executeWriteQuery(String sql, long start) {
        int affected = jdbcTemplate.update(sql);
        long elapsed = System.currentTimeMillis() - start;
        return new QueryResult(List.of(Map.of("affectedRows", affected)), affected, elapsed, false);
    }

    public record QueryResult(
            List<Map<String, Object>> rows,
            int rowCount,
            long executionTimeMs,
            boolean truncated
    ) {
    }
}
