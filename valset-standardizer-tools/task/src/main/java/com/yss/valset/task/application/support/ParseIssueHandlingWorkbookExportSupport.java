package com.yss.valset.task.application.support;

import com.yss.valset.task.application.dto.parseissue.FileParseRuleSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.FileParseSourceSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.ProductMatchRuleSheetRowDTO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 解析问题处理导出支持。
 */
@Component
public class ParseIssueHandlingWorkbookExportSupport {

    public byte[] exportFileParseSources(List<FileParseSourceSheetRowDTO> rows) {
        return export(
                "外部列指标映射",
                new String[]{
                        "ID",
                        "文件类型",
                        "标准列编码",
                        "标准列名称",
                        "来源列名称",
                        "扩展信息",
                        "启用状态",
                        "创建人",
                        "创建时间",
                        "修改人",
                        "修改时间",
                },
                rows,
                row -> new Object[]{
                        row.getId(),
                        row.getFileType(),
                        row.getColumnMap(),
                        row.getColumnMapName(),
                        row.getColumnName(),
                        row.getFileExtInfo(),
                        row.getStatus(),
                        row.getCreater(),
                        row.getCreateTime(),
                        row.getModifier(),
                        row.getModifyTime(),
                },
                row -> isDisabled(row.getStatus()),
                0
        );
    }

    public byte[] exportFileParseRules(List<FileParseRuleSheetRowDTO> rows) {
        return export(
                "标准列指标映射",
                new String[]{
                        "ID",
                        "文件场景",
                        "文件类型名称",
                        "标准区域名称",
                        "标准列编码",
                        "标准列名称",
                        "启用状态",
                        "是否多实例指标",
                        "是否必需",
                        "创建人",
                        "创建时间",
                        "修改人",
                        "修改时间",
                },
                rows,
                row -> new Object[]{
                        row.getId(),
                        row.getFileScene(),
                        row.getFileTypeName(),
                        row.getRegionName(),
                        row.getColumnMap(),
                        row.getColumnMapName(),
                        row.getStatus(),
                        row.getMultiIndex(),
                        row.getRequired(),
                        row.getCreater(),
                        row.getCreateTime(),
                        row.getModifier(),
                        row.getModifyTime(),
                },
                row -> isDisabled(row.getStatus()),
                0
        );
    }

    public byte[] exportProductMatchRules(List<ProductMatchRuleSheetRowDTO> rows) {
        return export(
                "产品识别规则配置表",
                new String[]{
                        "ID",
                        "文件类型名称",
                        "产品代码",
                        "产品名称",
                        "托管人代码",
                        "托管人名称",
                        "产品类型",
                        "科目体系",
                        "持仓状态",
                        "成立日",
                        "时效频率",
                        "延迟天数",
                        "是否审批",
                        "文件类型",
                        "匹配规则",
                        "启用状态",
                        "备注",
                        "调试名称",
                        "作业名称",
                        "作业场景",
                        "创建人",
                        "创建时间",
                        "修改人",
                        "修改时间",
                },
                rows,
                row -> new Object[]{
                        row.getId(),
                        row.getFileTypeName(),
                        row.getPdCd(),
                        row.getPdNm(),
                        row.getOrgCd(),
                        row.getOrgNm(),
                        row.getPdType(),
                        row.getSubjectSystem(),
                        row.getHoldingStatus(),
                        row.getEstablishedDate(),
                        row.getEffectiveFrequency(),
                        row.getDelayDays(),
                        row.getApprovalRequired(),
                        row.getFileType(),
                        row.getMatchRules(),
                        row.getIsValid(),
                        row.getMemo(),
                        row.getDebugName(),
                        row.getJobName(),
                        row.getJobScene(),
                        row.getCreater(),
                        row.getCreateTime(),
                        row.getModifier(),
                        row.getModifyTime(),
                },
                row -> isDisabled(row.getIsValid()),
                0
        );
    }

    private <T> byte[] export(String sheetName,
                              String[] headers,
                              List<T> rows,
                              Function<T, Object[]> rowMapper,
                              Predicate<T> disabledPredicate,
                              int... hiddenColumns) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle dataStyle = buildDataStyle(workbook, false);
            CellStyle disabledStyle = buildDataStyle(workbook, true);

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index += 1) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }

            if (rows != null) {
                for (int rowIndex = 0; rowIndex < rows.size(); rowIndex += 1) {
                    T row = rows.get(rowIndex);
                    Row excelRow = sheet.createRow(rowIndex + 1);
                    Object[] values = rowMapper.apply(row);
                    CellStyle rowStyle = disabledPredicate.test(row) ? disabledStyle : dataStyle;
                    for (int columnIndex = 0; columnIndex < headers.length; columnIndex += 1) {
                        Cell cell = excelRow.createCell(columnIndex);
                        setCellValue(cell, values == null || columnIndex >= values.length ? null : values[columnIndex]);
                        cell.setCellStyle(rowStyle);
                    }
                }
            }

            for (int columnIndex = 0; columnIndex < headers.length; columnIndex += 1) {
                sheet.autoSizeColumn(columnIndex);
                int width = Math.max(sheet.getColumnWidth(columnIndex), (headers[columnIndex].length() + 4) * 256);
                sheet.setColumnWidth(columnIndex, Math.min(255 * 256, width));
            }
            if (hiddenColumns != null) {
                for (int hiddenColumn : hiddenColumns) {
                    if (hiddenColumn >= 0 && hiddenColumn < headers.length) {
                        sheet.setColumnHidden(hiddenColumn, true);
                    }
                }
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出 Excel 失败", exception);
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle buildDataStyle(Workbook workbook, boolean disabled) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        if (disabled) {
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            if (style instanceof XSSFCellStyle && workbook instanceof XSSFWorkbook) {
                ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(
                        new java.awt.Color(255, 241, 240),
                        ((XSSFWorkbook) workbook).getStylesSource().getIndexedColors()));
            } else {
                style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            }
            Font font = workbook.createFont();
            if (font instanceof org.apache.poi.xssf.usermodel.XSSFFont && workbook instanceof XSSFWorkbook) {
                ((org.apache.poi.xssf.usermodel.XSSFFont) font).setColor(
                        new XSSFColor(new java.awt.Color(168, 7, 26),
                                ((XSSFWorkbook) workbook).getStylesSource().getIndexedColors()));
            } else {
                font.setColor(IndexedColors.DARK_RED.getIndex());
            }
            style.setFont(font);
        }
        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
            return;
        }
        if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
            return;
        }
        cell.setCellValue(String.valueOf(value));
    }

    private boolean isDisabled(Object value) {
        String text = normalize(value);
        return "0".equals(text)
                || "false".equalsIgnoreCase(text)
                || "no".equalsIgnoreCase(text)
                || "n".equalsIgnoreCase(text)
                || "停用".equals(text)
                || "否".equals(text);
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
