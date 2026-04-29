package com.yss.valset.transfer.application.impl.tagging;

import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferStatus;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTransferObjectBusinessFieldProjectionServiceTest {

    @Test
    void shouldProjectBusinessDateAndReceiveDateFromTagsAndReceiptTime() {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        TransferObject transferObject = new TransferObject(
                "transfer-1",
                "source-1",
                "EMAIL",
                "source-code",
                "2026年01月01日报表.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                123L,
                "fingerprint",
                "source-ref",
                "mail-id",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/tmp/report.xlsx",
                TransferStatus.RECEIVED,
                Instant.parse("2026-01-03T08:00:00Z"),
                Instant.parse("2026-01-03T08:01:00Z"),
                null,
                null,
                new ProbeResult(false, null, Map.of()),
                Map.of()
        );
        TransferObject persisted = transferObject.withBusinessFields(LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 3));
        when(transferObjectGateway.save(any())).thenReturn(persisted);

        TransferObject result = service.project(
                transferObject,
                List.of(new TransferObjectTag(
                        null,
                        "transfer-1",
                        "tag-1",
                        "BUSINESS_DATE",
                        "业务日期",
                        "2026年01月01日报表.xlsx",
                        "REGEX_RULE",
                        "正则命中",
                        "fileName",
                        "2026年01月01日报表.xlsx",
                        Map.of(),
                        Instant.now()
                ))
        );

        assertThat(result.businessDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.receiveDate()).isEqualTo(LocalDate.of(2026, 1, 3));
    }

    @Test
    void shouldOnlyFillReceiveDateWhenNoBusinessTagIsPresent() {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        TransferObject transferObject = new TransferObject(
                "transfer-2",
                "source-1",
                "EMAIL",
                "source-code",
                "plain-name.xlsx",
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                123L,
                "fingerprint-2",
                "source-ref",
                "mail-id",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/tmp/plain.xlsx",
                TransferStatus.RECEIVED,
                Instant.parse("2026-01-03T08:00:00Z"),
                Instant.parse("2026-01-03T08:01:00Z"),
                null,
                null,
                new ProbeResult(false, null, Map.of()),
                Map.of()
        );
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferObject result = service.project(transferObject, List.of());

        assertThat(result.businessDate()).isNull();
        assertThat(result.receiveDate()).isEqualTo(LocalDate.of(2026, 1, 3));
    }

    @Test
    void shouldExtractBusinessDateFromExcelFirstTenRows() throws IOException {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        Path filePath = Files.createTempFile("business-date", ".xlsx");
        createExcelFile(filePath, "无关内容", "2026-01-02");

        TransferObject transferObject = buildTransferObject("transfer-3", "report.xlsx", filePath.toString());
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferObject result = service.project(transferObject, List.of());

        assertThat(result.businessDate()).isEqualTo(LocalDate.of(2026, 1, 2));
    }

    @Test
    void shouldExtractBusinessDateFromXlsFirstTenRows() throws IOException {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        Path filePath = Files.createTempFile("business-date", ".xls");
        createXlsFile(filePath, "无关内容", "2026年01月04日");

        TransferObject transferObject = buildTransferObject("transfer-6", "report.xls", filePath.toString());
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferObject result = service.project(transferObject, List.of());

        assertThat(result.businessDate()).isEqualTo(LocalDate.of(2026, 1, 4));
    }

    @Test
    void shouldExtractBusinessDateFromCsvFirstTenRows() throws IOException {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        Path filePath = Files.createTempFile("business-date", ".csv");
        Files.writeString(filePath, "col1,col2\nfoo,bar\nbaz,20260103\n", StandardCharsets.UTF_8);

        TransferObject transferObject = buildTransferObject("transfer-4", "report.csv", filePath.toString());
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferObject result = service.project(transferObject, List.of());

        assertThat(result.businessDate()).isEqualTo(LocalDate.of(2026, 1, 3));
    }

    @Test
    void shouldKeepBusinessDateNullWhenNoDateAppearsInFileContent() throws IOException {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        Path filePath = Files.createTempFile("business-date", ".csv");
        Files.writeString(filePath, "col1,col2\nfoo,bar\nbaz,qux\n", StandardCharsets.UTF_8);

        TransferObject transferObject = buildTransferObject("transfer-5", "report.csv", filePath.toString());
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferObject result = service.project(transferObject, List.of());

        assertThat(result.businessDate()).isNull();
    }

    @Test
    void shouldIgnoreBusinessDateAfterTenthRow() throws IOException {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        Path filePath = Files.createTempFile("business-date", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(filePath)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            for (int rowIndex = 0; rowIndex < 10; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue("无关内容" + rowIndex);
            }
            Row row10 = sheet.createRow(10);
            row10.createCell(0).setCellValue("2026-01-05");
            workbook.write(outputStream);
        }

        TransferObject transferObject = buildTransferObject("transfer-7", "report.xlsx", filePath.toString());
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferObject result = service.project(transferObject, List.of());

        assertThat(result.businessDate()).isNull();
    }

    @Test
    void shouldIgnoreBusinessDateEarlierThanMinimumAllowedDate() throws IOException {
        TransferObjectGateway transferObjectGateway = mock(TransferObjectGateway.class);
        DefaultTransferObjectBusinessFieldProjectionService service = new DefaultTransferObjectBusinessFieldProjectionService(transferObjectGateway);

        Path filePath = Files.createTempFile("business-date", ".csv");
        Files.writeString(filePath, "col1,col2\nfoo,2015-12-31\nbaz,qux\n", StandardCharsets.UTF_8);

        TransferObject transferObject = buildTransferObject("transfer-8", "report.csv", filePath.toString());
        when(transferObjectGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransferObject result = service.project(transferObject, List.of());

        assertThat(result.businessDate()).isNull();
    }

    private TransferObject buildTransferObject(String transferId, String fileName, String localTempPath) {
        return new TransferObject(
                transferId,
                "source-1",
                "EMAIL",
                "source-code",
                fileName,
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                123L,
                "fingerprint",
                "source-ref",
                "mail-id",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                localTempPath,
                TransferStatus.RECEIVED,
                Instant.parse("2026-01-03T08:00:00Z"),
                Instant.parse("2026-01-03T08:01:00Z"),
                null,
                null,
                new ProbeResult(false, null, Map.of()),
                Map.of()
        );
    }

    private void createExcelFile(Path filePath, String firstRowValue, String secondRowValue) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(filePath)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue(firstRowValue);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(secondRowValue);
            workbook.write(outputStream);
        }
    }

    private void createXlsFile(Path filePath, String firstRowValue, String secondRowValue) throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(filePath)) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue(firstRowValue);
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(secondRowValue);
            workbook.write(outputStream);
        }
    }
}
