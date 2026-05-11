package com.wmatech.java_db_mcp.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SqlParserTest {

    private final SqlParser sqlParser = new SqlParser();

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM users",
            "select id, name from users where id = 1",
            "SELECT COUNT(*) FROM orders GROUP BY status",
            "SELECT u.name, o.total FROM users u JOIN orders o ON u.id = o.user_id"
    })
    void shouldDetectSelectQueries(String sql) {
        assertThat(sqlParser.parse(sql)).isEqualTo(QueryType.SELECT);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO users (name, email) VALUES ('John', 'john@test.com')",
            "insert into orders (product_id, qty) values (1, 5)"
    })
    void shouldDetectInsertQueries(String sql) {
        assertThat(sqlParser.parse(sql)).isEqualTo(QueryType.INSERT);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "UPDATE users SET name = 'Jane' WHERE id = 1",
            "update orders set status = 'shipped' where id = 42"
    })
    void shouldDetectUpdateQueries(String sql) {
        assertThat(sqlParser.parse(sql)).isEqualTo(QueryType.UPDATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DELETE FROM users WHERE id = 1",
            "delete from orders where status = 'cancelled'"
    })
    void shouldDetectDeleteQueries(String sql) {
        assertThat(sqlParser.parse(sql)).isEqualTo(QueryType.DELETE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100))",
            "ALTER TABLE users ADD COLUMN email VARCHAR(255)",
            "DROP TABLE users",
            "TRUNCATE TABLE orders",
            "CREATE INDEX idx_name ON users (name)"
    })
    void shouldDetectDdlQueries(String sql) {
        assertThat(sqlParser.parse(sql)).isEqualTo(QueryType.DDL);
    }

    @Test
    void shouldReturnOtherForUnparseableSql() {
        assertThat(sqlParser.parse("THIS IS NOT SQL")).isEqualTo(QueryType.OTHER);
    }

    @Test
    void shouldHandleCommentedSql() {
        assertThat(sqlParser.parse("/* comment */ SELECT * FROM users")).isEqualTo(QueryType.SELECT);
    }
}
