package com.wmatech.java_db_mcp.sql;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.stereotype.Component;

@Component
public class SqlParser {

    public QueryType parse(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return mapStatement(statement);
        } catch (Exception e) {
            return QueryType.OTHER;
        }
    }

    private QueryType mapStatement(Statement statement) {
        return switch (statement) {
            case Select _ -> QueryType.SELECT;
            case Insert _ -> QueryType.INSERT;
            case Update _ -> QueryType.UPDATE;
            case Delete _ -> QueryType.DELETE;
            case CreateTable _, CreateView _, CreateIndex _, Alter _, Drop _, Truncate _ -> QueryType.DDL;
            default -> QueryType.OTHER;
        };
    }
}
