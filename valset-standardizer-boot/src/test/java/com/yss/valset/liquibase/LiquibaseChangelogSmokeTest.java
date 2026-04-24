package com.yss.valset.liquibase;

import liquibase.Liquibase;
import liquibase.database.core.MockDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LiquibaseChangelogSmokeTest {

    private static final String MASTER_CHANGELOG = "db/changelog/db.changelog-master.xml";

    @Test
    void loadsMysqlPostgresAndOracleChangeSetsFromMasterChangelog() throws Exception {
        try (Liquibase liquibase = new Liquibase(MASTER_CHANGELOG, new ClassLoaderResourceAccessor(), new MockDatabase())) {
            Set<String> changeSetPaths = liquibase.getDatabaseChangeLog().getChangeSets().stream()
                    .map(changeSet -> changeSet.getFilePath())
                    .collect(Collectors.toSet());

            assertThat(liquibase.getDatabaseChangeLog().getChangeSets()).hasSize(54);
            assertThat(changeSetPaths).contains(
                    "db/changelog/sql/common/core.sql",
                    "db/changelog/sql/common/ods.sql",
                    "db/changelog/sql/common/dwd.sql",
                    "db/changelog/sql/common/knowledge.sql",
                    "db/changelog/sql/common/transfer.sql",
                    "db/changelog/sql/common/db-scheduler.sql",
                    "db/changelog/sql/common/transfer-email.sql",
                    "db/changelog/sql/common/transfer-init.sql",
                    "db/changelog/sql/common/indexes.sql"
            );
        }
    }
}
