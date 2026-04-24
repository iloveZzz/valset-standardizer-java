package com.yss.valset.liquibase;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class LiquibaseContainerMigrationTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("valset_standardizer")
            .withUsername("valset_standardizer")
            .withPassword("valset_standardizer");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("valset_standardizer")
            .withUsername("valset_standardizer")
            .withPassword("valset_standardizer");

    @Test
    void migratesMysqlSchema() throws Exception {
        verifyMigration(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword(),
                "select count(*) from information_schema.tables where table_schema = database() and table_name in ('leaf_alloc','t_subject_match_file_info','t_ods_valuation_sheet_style','t_stg_external_valuation','t_dwd_external_valuation_subject','t_dwd_external_valuation_metric','t_ods_standard_subject','t_transfer_object','t_transfer_rule','t_transfer_route','t_transfer_delivery_record','t_transfer_target','scheduled_tasks')",
                "select count(*) from information_schema.statistics where table_schema = database() and table_name = 't_transfer_object' and index_name = 'uk_transfer_object_fingerprint'",
                "select count(*) from information_schema.columns where table_schema = database() and table_name = 't_transfer_object' and column_name = 'mail_subject'",
                "select count(*) from t_transfer_target",
                "select count(*) from t_transfer_rule"
        );
    }

    @Test
    void migratesPostgresSchema() throws Exception {
        verifyMigration(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name in ('leaf_alloc','t_subject_match_file_info','t_ods_valuation_sheet_style','t_stg_external_valuation','t_dwd_external_valuation_subject','t_dwd_external_valuation_metric','t_ods_standard_subject','t_transfer_object','t_transfer_rule','t_transfer_route','t_transfer_delivery_record','t_transfer_target','scheduled_tasks')",
                "select count(*) from pg_indexes where schemaname = 'public' and tablename = 't_transfer_object' and indexname = 'uk_transfer_object_fingerprint'",
                "select count(*) from information_schema.columns where table_schema = 'public' and table_name = 't_transfer_object' and column_name = 'mail_subject'",
                "select count(*) from t_transfer_target",
                "select count(*) from t_transfer_rule"
        );
    }

    private void verifyMigration(String jdbcUrl, String username, String password, String tableCountSql, String indexCountSql, String mailColumnSql, String targetCountSql, String ruleCountSql) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new liquibase.resource.ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }

        try (Connection verifyConnection = DriverManager.getConnection(jdbcUrl, username, password)) {
            assertThat(scalarCount(verifyConnection, tableCountSql)).isGreaterThanOrEqualTo(23);
            assertThat(scalarCount(verifyConnection, indexCountSql)).isEqualTo(1);
            assertThat(scalarCount(verifyConnection, mailColumnSql)).isGreaterThanOrEqualTo(1);
            assertThat(scalarCount(verifyConnection, targetCountSql)).isEqualTo(6);
            assertThat(scalarCount(verifyConnection, ruleCountSql)).isEqualTo(3);
        }
    }

    private long scalarCount(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new IllegalStateException("No rows returned for query: " + sql);
        }
    }
}
