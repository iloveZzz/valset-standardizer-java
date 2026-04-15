package com.yss.subjectmatch.extract.repository.mapper;

import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import net.jqwik.api.*;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Tag;

import java.io.Reader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ValuationFileDataMapper persistence integrity.
 * 
 * **Validates: Requirements 3.1, 3.2, 3.5**
 */
@Tag("Feature: ods-data-extraction, Property 2: Persistence Integrity")
public class ValuationFileDataMapperPropertyTest {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            // Initialize MyBatis with H2 in-memory database
            Reader reader = Resources.getResourceAsReader("mybatis-config-test.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            
            // Create schema
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Connection conn = session.getConnection();
                ScriptRunner runner = new ScriptRunner(conn);
                runner.setLogWriter(null);
                Reader schemaReader = Resources.getResourceAsReader("schema-test.sql");
                runner.runScript(schemaReader);
                schemaReader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize test database", e);
        }
    }

    /**
     * Property 2: Persistence Integrity
     * 
     * For any batch of RawRowData records inserted for a given task_id,
     * querying findByTaskId(taskId) should return exactly those records with
     * correct row_data_number values starting from 1 and incrementing sequentially,
     * and each record's row_data_json should match the original serialized value.
     */
    @Property(tries = 100)
    void persistenceIntegrity(
            @ForAll("rawRowDataLists") List<ValuationFileDataPO> rawRows
    ) throws Exception {
        // Given: arbitrary list of raw row data with random task_id/file_id
        if (rawRows.isEmpty()) {
            return; // Skip empty lists
        }

        Long taskId = rawRows.get(0).getTaskId();
        int expectedCount = rawRows.size();

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ValuationFileDataMapper mapper = session.getMapper(ValuationFileDataMapper.class);
            
            // Clean up all data before this test to ensure isolation
            session.getConnection().createStatement().execute("TRUNCATE TABLE t_ods_valuation_filedata");
            
            // When: insert via ValuationFileDataMapper
            mapper.insert(rawRows, 100);

            // Then: query back and verify
            List<ValuationFileDataPO> retrieved = mapper.findByTaskId(taskId);

            // Assert: count equals inserted count
            assertThat(retrieved)
                    .as("Retrieved count should match inserted count")
                    .hasSize(expectedCount);

            // Assert: row numbers are sequential from 1
            for (int i = 0; i < retrieved.size(); i++) {
                assertThat(retrieved.get(i).getRowDataNumber())
                        .as("Row number should be sequential starting from 1")
                        .isEqualTo(i + 1);
            }

            // Assert: each row_data_json matches the original
            for (int i = 0; i < retrieved.size(); i++) {
                ValuationFileDataPO original = rawRows.get(i);
                ValuationFileDataPO persisted = retrieved.get(i);

                assertThat(persisted.getRowDataJson())
                        .as("Row data JSON should match original for row " + (i + 1))
                        .isEqualTo(original.getRowDataJson());

                assertThat(persisted.getTaskId())
                        .as("Task ID should match original")
                        .isEqualTo(original.getTaskId());

                assertThat(persisted.getFileId())
                        .as("File ID should match original")
                        .isEqualTo(original.getFileId());
            }
        }
    }

    /**
     * Provides arbitrary lists of ValuationFileDataPO with random task_id/file_id.
     */
    @Provide
    Arbitrary<List<ValuationFileDataPO>> rawRowDataLists() {
        Arbitrary<Long> taskIds = Arbitraries.longs().between(1L, 1000L);
        Arbitrary<Long> fileIds = Arbitraries.longs().between(1L, 100L);
        Arbitrary<String> jsonData = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('[', ']', ',', '"', '.', '-')
                .ofMinLength(10)
                .ofMaxLength(200)
                .map(s -> "[\"" + s + "\"]"); // Wrap in JSON array format

        return Combinators.combine(taskIds, fileIds)
                .as((taskId, fileId) -> {
                    // Generate 1-50 rows for the same task_id and file_id
                    int rowCount = Arbitraries.integers().between(1, 50).sample();
                    List<ValuationFileDataPO> rows = new ArrayList<>();
                    
                    for (int i = 1; i <= rowCount; i++) {
                        ValuationFileDataPO po = new ValuationFileDataPO();
                        po.setTaskId(taskId);
                        po.setFileId(fileId);
                        po.setRowDataNumber(i);
                        po.setRowDataJson(jsonData.sample());
                        rows.add(po);
                    }
                    
                    return rows;
                });
    }
}
