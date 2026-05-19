package com.yss.valset.extract.parser.file;

import java.nio.file.Path;
import java.util.List;

/**
 * 文件读取后进入规则解析前的原始二维表。
 */
final class ValuationRawTable {

    private final Path sourcePath;
    private final String sheetName;
    private final List<List<Object>> rows;

    private ValuationRawTable(Path sourcePath, String sheetName, List<List<Object>> rows) {
        this.sourcePath = sourcePath;
        this.sheetName = sheetName;
        this.rows = rows;
    }

    static ValuationRawTable of(Path sourcePath, String sheetName, List<List<Object>> rows) {
        return new ValuationRawTable(sourcePath, sheetName, rows);
    }

    Path sourcePath() {
        return sourcePath;
    }

    String sheetName() {
        return sheetName;
    }

    List<List<Object>> rows() {
        return rows;
    }

    boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }
}
