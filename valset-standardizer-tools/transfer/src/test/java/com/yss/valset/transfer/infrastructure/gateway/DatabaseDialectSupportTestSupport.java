package com.yss.valset.transfer.infrastructure.gateway;

import com.yss.valset.common.support.DatabaseDialectSupport;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class DatabaseDialectSupportTestSupport {

    private DatabaseDialectSupportTestSupport() {
    }

    static DatabaseDialectSupport mysql() {
        try {
            DataSource dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(databaseMetaData);
            when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");
            doNothing().when(connection).close();
            return new DatabaseDialectSupport(dataSource);
        } catch (Exception exception) {
            throw new IllegalStateException("构造测试数据源失败", exception);
        }
    }
}
