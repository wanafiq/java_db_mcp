package com.wmatech.java_db_mcp.sql;

import com.wmatech.java_db_mcp.common.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionServiceTest {

    private final SqlParser sqlParser = new SqlParser();

    @Test
    void shouldAlwaysAllowSelect() {
        var service = createService(false, false, false, false);
        assertThat(service.validateAndGetType("SELECT * FROM users")).isEqualTo(QueryType.SELECT);
    }

    @Test
    void shouldAllowInsertWhenEnabled() {
        var service = createService(true, false, false, false);
        assertThat(service.validateAndGetType("INSERT INTO users (name) VALUES ('test')")).isEqualTo(QueryType.INSERT);
    }

    @Test
    void shouldDenyInsertWhenDisabled() {
        var service = createService(false, false, false, false);
        assertThatThrownBy(() -> service.validateAndGetType("INSERT INTO users (name) VALUES ('test')"))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("INSERT");
    }

    @Test
    void shouldAllowUpdateWhenEnabled() {
        var service = createService(false, true, false, false);
        assertThat(service.validateAndGetType("UPDATE users SET name = 'test' WHERE id = 1")).isEqualTo(QueryType.UPDATE);
    }

    @Test
    void shouldDenyUpdateWhenDisabled() {
        var service = createService(false, false, false, false);
        assertThatThrownBy(() -> service.validateAndGetType("UPDATE users SET name = 'test' WHERE id = 1"))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("UPDATE");
    }

    @Test
    void shouldAllowDeleteWhenEnabled() {
        var service = createService(false, false, true, false);
        assertThat(service.validateAndGetType("DELETE FROM users WHERE id = 1")).isEqualTo(QueryType.DELETE);
    }

    @Test
    void shouldDenyDeleteWhenDisabled() {
        var service = createService(false, false, false, false);
        assertThatThrownBy(() -> service.validateAndGetType("DELETE FROM users WHERE id = 1"))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("DELETE");
    }

    @Test
    void shouldAllowDdlWhenEnabled() {
        var service = createService(false, false, false, true);
        assertThat(service.validateAndGetType("CREATE TABLE test (id INT)")).isEqualTo(QueryType.DDL);
    }

    @Test
    void shouldDenyDdlWhenDisabled() {
        var service = createService(false, false, false, false);
        assertThatThrownBy(() -> service.validateAndGetType("CREATE TABLE test (id INT)"))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("DDL");
    }

    @Test
    void shouldDenyUnparseableSql() {
        var service = createService(true, true, true, true);
        assertThatThrownBy(() -> service.validateAndGetType("NOT VALID SQL"))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void shouldDescribePermissions() {
        var service = createService(true, false, true, false);
        String description = service.describePermissions();
        assertThat(description)
                .contains("SELECT")
                .contains("INSERT")
                .contains("DELETE")
                .doesNotContain("UPDATE")
                .doesNotContain("DDL");
    }

    private PermissionService createService(boolean insert, boolean update, boolean delete, boolean ddl) {
        return new PermissionService(sqlParser, new SqlPermissionProperties(insert, update, delete, ddl));
    }
}
