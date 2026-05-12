package com.yss.valset.common.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Locale;

/**
 * 数据库方言识别与分页尾缀生成支持。
 */
@Component
@RequiredArgsConstructor
public class DatabaseDialectSupport {

    private final DataSource dataSource;

    private volatile String databaseProductName;

    /**
     * 是否是 Oracle 数据库。
     */
    public boolean isOracle() {
        return resolveDatabaseProductName().contains("oracle");
    }

    /**
     * 是否是 PostgreSQL 数据库。
     */
    public boolean isPostgreSql() {
        return resolveDatabaseProductName().contains("postgresql");
    }

    /**
     * 生成分页尾缀。Oracle 使用 fetch next 语法，其它数据库沿用 limit 语法。
     */
    public String limitClause(Integer limit) {
        if (limit == null || limit <= 0) {
            return null;
        }
        return isOracle()
                ? "fetch first " + limit + " rows only"
                : "limit " + limit;
    }

    private String resolveDatabaseProductName() {
        String cached = databaseProductName;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (databaseProductName == null) {
                databaseProductName = detectDatabaseProductName();
            }
            return databaseProductName;
        }
    }

    private String detectDatabaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            String productName = databaseMetaData == null ? null : databaseMetaData.getDatabaseProductName();
            return normalize(productName);
        } catch (Exception exception) {
            throw new IllegalStateException("识别数据库方言失败", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
