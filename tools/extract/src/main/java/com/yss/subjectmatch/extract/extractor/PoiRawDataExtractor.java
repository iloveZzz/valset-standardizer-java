package com.yss.subjectmatch.extract.extractor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.exception.FileAccessException;
import com.yss.subjectmatch.domain.extractor.RawDataExtractor;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.entity.ValuationSheetStylePO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.subjectmatch.extract.repository.mapper.ValuationSheetStyleMapper;
import com.yss.subjectmatch.extract.support.ExcelUniverSnapshotSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.enums.CellExtraTypeEnum;
import org.apache.fesod.sheet.metadata.CellExtra;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.apache.fesod.sheet.read.metadata.holder.ReadRowHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 Fesod 的 Excel 原始数据提取器。
 * <p>
 * 该实现同时支持 .xls 和 .xlsx，并通过监听器按行读取后落入 ODS 原始表。
 * 行数据会保留列顺序、空列占位以及合并表头所需的原始列结构。
 * <br>
 * 除了原始行值之外，还会补充 Univer 兼容的行级单元格快照和表头预览信息，
 * 供前端直接渲染 Excel 样式与合并区域。
 * </p>
 */
@Slf4j
@Component
public class PoiRawDataExtractor implements RawDataExtractor {

    private static final int BATCH_SIZE = 1000;
    private static final int HEADER_PREVIEW_ROWS = 20;
    private static final int MAX_COLUMNS = 10000;

    private final ValuationFileDataMapper valuationFileDataMapper;
    private final ObjectMapper objectMapper;
    private final ValuationSheetStyleMapper valuationSheetStyleMapper;
    @Value("${subject.match.workflow.skip-excel-style-parsing:false}")
    private boolean skipExcelStyleParsing;

    public PoiRawDataExtractor(ValuationFileDataMapper valuationFileDataMapper, ObjectMapper objectMapper) {
        this(valuationFileDataMapper, objectMapper, null);
    }

    public PoiRawDataExtractor(ValuationFileDataMapper valuationFileDataMapper,
                               ObjectMapper objectMapper,
                               @Nullable ValuationSheetStyleMapper valuationSheetStyleMapper) {
        this.valuationFileDataMapper = valuationFileDataMapper;
        this.objectMapper = objectMapper;
        this.valuationSheetStyleMapper = valuationSheetStyleMapper;
    }

    @Override
    public int extract(DataSourceConfig config, Long taskId, Long fileId) {
        if (taskId == null || fileId == null) {
            throw new IllegalArgumentException("taskId and fileId must not be null");
        }

        Path filePath = Paths.get(config.getSourceUri());
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileAccessException(config.getSourceUri());
        }

        long startedAt = System.currentTimeMillis();
        try (ExcelUniverSnapshotSupport snapshotSupport = skipExcelStyleParsing ? null : new ExcelUniverSnapshotSupport(filePath)) {
            FesodRawRowListener listener = new FesodRawRowListener(taskId, fileId, snapshotSupport, skipExcelStyleParsing);
            FesodSheet.read(filePath.toString(), listener)
                    .headRowNumber(0)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .sheet()
                    .doRead();
            log.info("Excel 原始数据提取完成，filePath={}, rowCount={}, durationMs={}",
                    filePath, listener.getPersistedRows(), System.currentTimeMillis() - startedAt);
            return listener.getPersistedRows();
        } catch (FileAccessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("使用 Fesod 读取 Excel 原始数据失败，filePath=" + filePath, exception);
        }
    }

    @Override
    public DataSourceType supportedType() {
        return DataSourceType.EXCEL;
    }

    private final class FesodRawRowListener implements ReadListener<Map<Integer, String>> {

        private final Long taskId;
        private final Long fileId;
        private final ExcelUniverSnapshotSupport snapshotSupport;
        private final boolean skipExcelStyleParsing;
        private final List<ValuationFileDataPO> batch = new ArrayList<>(BATCH_SIZE);
        private final List<ExcelUniverSnapshotSupport.RowSnapshot> headerPreviewRows = new ArrayList<>();
        private String currentSheetName;
        private boolean headerMetaReady;
        private int rowSequence;
        private int persistedRows;

        private FesodRawRowListener(Long taskId, Long fileId, ExcelUniverSnapshotSupport snapshotSupport, boolean skipExcelStyleParsing) {
            this.taskId = taskId;
            this.fileId = fileId;
            this.snapshotSupport = snapshotSupport;
            this.skipExcelStyleParsing = skipExcelStyleParsing;
        }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            if (log.isDebugEnabled()) {
                log.debug("读取到 Excel 表头，sheet={}, rowNum={}, headSize={}",
                        currentSheetName(context), context == null ? null : context.getCurrentRowNum(),
                        headMap == null ? 0 : headMap.size());
            }
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            if (data == null || data.isEmpty()) {
                return;
            }

            String sheetName = currentSheetName(context);
            if (sheetName == null) {
                sheetName = "Sheet1";
            }
            if (currentSheetName != null && !currentSheetName.equals(sheetName)) {
                flushCurrentSheet();
            }
            if (currentSheetName == null) {
                currentSheetName = sheetName;
            }

            List<String> rowValues = buildDenseRow(data, context);
            if (rowValues.isEmpty() || rowValues.stream().allMatch(value -> value == null || value.isBlank())) {
                return;
            }

            int rowDataNumber = ++rowSequence;
            int sheetRowIndex = context == null ? rowDataNumber - 1 : Math.max(context.getCurrentRowNum(), 0);

            try {
                boolean includeStyle = !skipExcelStyleParsing && rowDataNumber <= HEADER_PREVIEW_ROWS;
                ExcelUniverSnapshotSupport.RowSnapshot rowSnapshot = buildRowSnapshot(sheetRowIndex, rowValues, includeStyle);

                if (!skipExcelStyleParsing && !headerMetaReady && headerPreviewRows.size() < HEADER_PREVIEW_ROWS) {
                    headerPreviewRows.add(rowSnapshot);
                }

                ValuationFileDataPO po = new ValuationFileDataPO();
                po.setTaskId(taskId);
                po.setFileId(fileId);
                po.setSheetName(currentSheetName);
                po.setRowDataNumber(rowDataNumber);
                po.setRowDataJson(objectMapper.writeValueAsString(rowValues));
                po.setRowUniverJson(objectMapper.writeValueAsString(rowSnapshot));
                batch.add(po);
                persistedRows++;

                if (!skipExcelStyleParsing && !headerMetaReady && headerPreviewRows.size() >= HEADER_PREVIEW_ROWS) {
                    attachHeaderMetaIfNecessary();
                }

                if (batch.size() >= BATCH_SIZE) {
                    flushBatch();
                }
            } catch (JsonProcessingException exception) {
                log.error("第 {} 行序列化为 JSON 失败，已跳过", rowDataNumber, exception);
            }
        }

        @Override
        public void extra(CellExtra extra, AnalysisContext context) {
            if (extra != null && extra.getType() == CellExtraTypeEnum.MERGE && log.isDebugEnabled()) {
                log.debug("读取到合并单元格信息，sheet={}, firstRow={}, lastRow={}, firstColumn={}, lastColumn={}",
                        currentSheetName(context),
                        extra.getFirstRowIndex(),
                        extra.getLastRowIndex(),
                        extra.getFirstColumnIndex(),
                        extra.getLastColumnIndex());
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            flushCurrentSheet();
            log.info("Excel 原始数据写入完成，sheet={}, persistedRows={}",
                    currentSheetName(context), persistedRows);
        }

        @Override
        public boolean hasNext(AnalysisContext context) {
            return true;
        }

        @Override
        public void onException(Exception exception, AnalysisContext context) throws Exception {
            log.error("Fesod 读取 Excel 原始数据异常，sheet={}, rowNum={}",
                    currentSheetName(context),
                    context == null ? null : context.getCurrentRowNum(),
                    exception);
            throw exception;
        }

        private void flushCurrentSheet() {
            if (!skipExcelStyleParsing) {
                attachHeaderMetaIfNecessary();
            }
            flushBatch();
            headerPreviewRows.clear();
            headerMetaReady = false;
            currentSheetName = null;
        }

        private void attachHeaderMetaIfNecessary() {
            if (skipExcelStyleParsing || headerMetaReady || headerPreviewRows.isEmpty()) {
                return;
            }
            try {
                String headerMetaJson = objectMapper.writeValueAsString(
                        snapshotSupport.buildHeaderMeta(currentSheetName, new ArrayList<>(headerPreviewRows)));
                persistSheetStyleSnapshot(headerMetaJson);
                if (!batch.isEmpty()) {
                    batch.get(0).setHeaderMetaJson(headerMetaJson);
                }
                headerMetaReady = true;
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("构建 Univer 表头元数据失败，sheet=" + currentSheetName, exception);
            }
        }

        private void flushBatch() {
            if (batch.isEmpty()) {
                return;
            }
            valuationFileDataMapper.insert(new ArrayList<>(batch), BATCH_SIZE);
            batch.clear();
        }

        private void persistSheetStyleSnapshot(String headerMetaJson) {
            if (skipExcelStyleParsing || valuationSheetStyleMapper == null) {
                return;
            }
            ValuationSheetStylePO stylePO = new ValuationSheetStylePO();
            stylePO.setTaskId(taskId);
            stylePO.setFileId(fileId);
            stylePO.setSheetName(currentSheetName);
            stylePO.setStyleScope("HEADER_PREVIEW");
            stylePO.setSheetStyleJson(headerMetaJson);
            stylePO.setPreviewRowCount(headerPreviewRows.size());
            stylePO.setCreatedAt(java.time.LocalDateTime.now());
            valuationSheetStyleMapper.insert(stylePO);
        }

        private ExcelUniverSnapshotSupport.RowSnapshot buildRowSnapshot(int sheetRowIndex,
                                                                       List<String> rowValues,
                                                                       boolean includeStyle) {
            if (!skipExcelStyleParsing && snapshotSupport != null) {
                return snapshotSupport.buildRowSnapshot(currentSheetName, sheetRowIndex, rowValues, includeStyle);
            }
            return buildMinimalRowSnapshot(currentSheetName, sheetRowIndex, rowValues);
        }

        private ExcelUniverSnapshotSupport.RowSnapshot buildMinimalRowSnapshot(String sheetName, int rowIndex, List<String> rowValues) {
            Map<Integer, Map<String, Object>> rowCellData = new java.util.LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < rowValues.size(); columnIndex++) {
                String value = rowValues.get(columnIndex);
                if (value == null) {
                    continue;
                }
                Map<String, Object> cellData = new java.util.LinkedHashMap<>();
                cellData.put("v", value);
                rowCellData.put(columnIndex, cellData);
            }
            return new ExcelUniverSnapshotSupport.RowSnapshot(sheetName, rowIndex, rowCellData);
        }

        private List<String> buildDenseRow(Map<Integer, String> data, AnalysisContext context) {
            int maxColumnIndex = data.keySet().stream()
                    .filter(index -> index != null && index >= 0)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(-1);

            ReadRowHolder rowHolder = context == null ? null : context.readRowHolder();
            if (rowHolder != null && rowHolder.getCellMap() != null && !rowHolder.getCellMap().isEmpty()) {
                int rowHolderMaxIndex = rowHolder.getCellMap().keySet().stream()
                        .filter(index -> index != null && index >= 0)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(-1);
                maxColumnIndex = Math.max(maxColumnIndex, rowHolderMaxIndex);
            }

            if (maxColumnIndex < 0) {
                return List.of();
            }

            if (maxColumnIndex + 1 > MAX_COLUMNS) {
                log.warn("Excel 行列数超过上限 {}，已截断为 {} 列，sheet={}, rowNum={}",
                        MAX_COLUMNS, MAX_COLUMNS, currentSheetName(context),
                        context == null ? null : context.getCurrentRowNum());
                maxColumnIndex = MAX_COLUMNS - 1;
            }

            List<String> rowValues = new ArrayList<>(Collections.nCopies(maxColumnIndex + 1, null));
            for (Map.Entry<Integer, String> entry : data.entrySet()) {
                Integer columnIndex = entry.getKey();
                if (columnIndex == null || columnIndex < 0 || columnIndex > maxColumnIndex) {
                    continue;
                }
                rowValues.set(columnIndex, normalizeCellValue(entry.getValue()));
            }
            return rowValues;
        }

        private String normalizeCellValue(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(trimmed.replace(",", "")).stripTrailingZeros().toPlainString();
            } catch (NumberFormatException ignored) {
                return trimmed;
            }
        }

        private String currentSheetName(AnalysisContext context) {
            if (context == null || context.readSheetHolder() == null) {
                return currentSheetName;
            }
            return context.readSheetHolder().getSheetName();
        }

        private int getPersistedRows() {
            return persistedRows;
        }
    }
}
