package com.yss.valset.common.support;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SpreadsheetML / Excel 2003 XML 的解析辅助器。
 */
public final class SpreadsheetXmlSupport {

    private static final String SPREADSHEET_XML_NS = "urn:schemas-microsoft-com:office:spreadsheet";
    private static final String MSO_APPLICATION_MARKER = "<?mso-application progid=\"Excel.Sheet\"?>";
    private static final int HEADER_PROBE_SIZE = 8192;

    private SpreadsheetXmlSupport() {
    }

    /**
     * Java 8 兼容的 readNBytes 方法。
     */
    private static byte[] readNBytes(InputStream inputStream, int length) throws IOException {
        byte[] buffer = new byte[length];
        int totalRead = 0;
        while (totalRead < length) {
            int read = inputStream.read(buffer, totalRead, length - totalRead);
            if (read == -1) {
                break;
            }
            totalRead += read;
        }
        if (totalRead < length) {
            byte[] result = new byte[totalRead];
            System.arraycopy(buffer, 0, result, 0, totalRead);
            return result;
        }
        return buffer;
    }

    /**
     * 判断文件是否是 SpreadsheetML。
     */
    public static boolean isSpreadsheetXml(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] headerBytes = readNBytes(inputStream, HEADER_PROBE_SIZE);
            return isSpreadsheetXml(headerBytes);
        }
    }

    /**
     * 判断字节内容是否是 SpreadsheetML。
     */
    public static boolean isSpreadsheetXml(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        return containsSpreadsheetXmlMarker(bytes, StandardCharsets.UTF_8)
                || containsSpreadsheetXmlMarker(bytes, StandardCharsets.UTF_16LE)
                || containsSpreadsheetXmlMarker(bytes, StandardCharsets.UTF_16BE);
    }

    /**
     * 读取整个 SpreadsheetML 工作簿。
     */
    public static SpreadsheetXmlWorkbook read(Path filePath) throws IOException {
        return read(Files.readAllBytes(filePath));
    }

    /**
     * 读取整个 SpreadsheetML 工作簿。
     */
    public static SpreadsheetXmlWorkbook read(byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
            NodeList worksheetNodes = document.getElementsByTagName("Worksheet");
            if (worksheetNodes == null || worksheetNodes.getLength() == 0) {
                throw new IllegalStateException("未找到 SpreadsheetML 工作表");
            }

            List<SpreadsheetXmlSheet> sheets = new ArrayList<>(worksheetNodes.getLength());
            for (int sheetIndex = 0; sheetIndex < worksheetNodes.getLength(); sheetIndex++) {
                Element worksheet = (Element) worksheetNodes.item(sheetIndex);
                String sheetName = resolveSheetName(worksheet, sheetIndex);
                Element table = firstChildElementByTagName(worksheet, "Table");
                if (table == null) {
                    throw new IllegalStateException("SpreadsheetML 工作表缺少 Table 节点，sheet=" + sheetName);
                }
                sheets.add(readSheet(sheetName, table));
            }
            return new SpreadsheetXmlWorkbook(sheets);
        } catch (Exception exception) {
            throw new IllegalStateException("读取 SpreadsheetML 工作簿失败", exception);
        }
    }

    /**
     * 读取首个工作表的行数据。
     */
    public static List<List<String>> readFirstSheetRows(Path filePath) throws IOException {
        SpreadsheetXmlWorkbook workbook = read(filePath);
        if (workbook.sheets().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return workbook.sheets().get(0).rows();
    }

    /**
     * 读取首个工作表的行数据。
     */
    public static List<List<String>> readFirstSheetRows(byte[] bytes) {
        SpreadsheetXmlWorkbook workbook = read(bytes);
        if (workbook.sheets().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return workbook.sheets().get(0).rows();
    }

    private static boolean containsSpreadsheetXmlMarker(byte[] bytes, Charset charset) {
        String header = new String(bytes, 0, Math.min(bytes.length, HEADER_PROBE_SIZE), charset);
        return header.contains(SPREADSHEET_XML_NS)
                || header.contains(MSO_APPLICATION_MARKER)
                || header.contains("<Workbook");
    }

    private static SpreadsheetXmlSheet readSheet(String sheetName, Element table) {
        List<List<String>> rows = new ArrayList<>();
        List<SpreadsheetXmlMergeRegion> mergeRegions = new ArrayList<>();
        NodeList childNodes = table.getChildNodes();
        for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
            Node node = childNodes.item(childIndex);
            if (!(node instanceof Element) || !"Row".equals(((Element) node).getTagName())) {
                continue;
            }
            Element rowElement = (Element) node;

            int targetRowIndex = rows.size();
            String rowIndexAttr = rowElement.getAttribute("ss:Index");
            if (rowIndexAttr != null && !rowIndexAttr.trim().isEmpty()) {
                targetRowIndex = parseIndex(rowIndexAttr) - 1;
                while (rows.size() < targetRowIndex) {
                    rows.add(java.util.Arrays.asList());
                }
            }

            List<String> rowValues = new ArrayList<>();
            int nextCellIndex = 0;
            NodeList cellNodes = rowElement.getChildNodes();
            for (int cellNodeIndex = 0; cellNodeIndex < cellNodes.getLength(); cellNodeIndex++) {
                Node cellNode = cellNodes.item(cellNodeIndex);
                if (!(cellNode instanceof Element) || !"Cell".equals(((Element) cellNode).getTagName())) {
                    continue;
                }
                Element cellElement = (Element) cellNode;

                String cellIndexAttr = cellElement.getAttribute("ss:Index");
                if (cellIndexAttr != null && !cellIndexAttr.trim().isEmpty()) {
                    nextCellIndex = parseIndex(cellIndexAttr) - 1;
                }
                while (rowValues.size() < nextCellIndex) {
                    rowValues.add("");
                }

                rowValues.add(extractCellValue(cellElement));

                int mergeAcross = parseOptionalIndex(cellElement.getAttribute("ss:MergeAcross"));
                int mergeDown = parseOptionalIndex(cellElement.getAttribute("ss:MergeDown"));
                if (mergeAcross > 0 || mergeDown > 0) {
                    mergeRegions.add(new SpreadsheetXmlMergeRegion(
                            targetRowIndex,
                            nextCellIndex,
                            targetRowIndex + mergeDown,
                            nextCellIndex + mergeAcross
                    ));
                }
                for (int mergeIndex = 0; mergeIndex < mergeAcross; mergeIndex++) {
                    rowValues.add("");
                }
                nextCellIndex += mergeAcross + 1;
            }

            rows.add(rowValues);
        }

        return new SpreadsheetXmlSheet(sheetName, rows, mergeRegions);
    }

    private static String resolveSheetName(Element worksheet, int sheetIndex) {
        String sheetName = worksheet.getAttribute("ss:Name");
        if (sheetName == null || sheetName.trim().isEmpty()) {
            sheetName = worksheet.getAttribute("Name");
        }
        if (sheetName == null || sheetName.trim().isEmpty()) {
            sheetName = "Sheet" + (sheetIndex + 1);
        }
        return sheetName;
    }

    private static Element firstChildElementByTagName(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList == null || nodeList.getLength() == 0) {
            return null;
        }
        return (Element) nodeList.item(0);
    }

    private static String extractCellValue(Element cellElement) {
        NodeList dataNodes = cellElement.getElementsByTagName("Data");
        if (dataNodes == null || dataNodes.getLength() == 0) {
            return "";
        }
        String value = dataNodes.item(0).getTextContent();
        return value == null ? "" : value.trim();
    }

    private static int parseIndex(String value) {
        return Integer.parseInt(value.trim());
    }

    private static int parseOptionalIndex(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return parseIndex(value);
    }

    /**
     * SpreadsheetML 工作簿。
     */
    public static final class SpreadsheetXmlWorkbook {
        private final List<SpreadsheetXmlSheet> sheets;

        public SpreadsheetXmlWorkbook(List<SpreadsheetXmlSheet> sheets) {
            this.sheets = sheets == null ? java.util.Collections.<SpreadsheetXmlSheet>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(sheets));
        }

        public List<SpreadsheetXmlSheet> sheets() {
            return sheets;
        }
    }

    /**
     * SpreadsheetML 工作表。
     */
    public static final class SpreadsheetXmlSheet {
        private final String sheetName;
        private final List<List<String>> rows;
        private final List<SpreadsheetXmlMergeRegion> mergeRegions;

        public SpreadsheetXmlSheet(String sheetName,
                                   List<List<String>> rows,
                                   List<SpreadsheetXmlMergeRegion> mergeRegions) {
            this.sheetName = sheetName;
            this.rows = rows == null ? java.util.Collections.<List<String>>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(rows));
            this.mergeRegions = mergeRegions == null ? java.util.Collections.<SpreadsheetXmlMergeRegion>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(mergeRegions));
        }

        public String sheetName() {
            return sheetName;
        }

        public List<List<String>> rows() {
            return rows;
        }

        public List<SpreadsheetXmlMergeRegion> mergeRegions() {
            return mergeRegions;
        }
    }

    /**
     * SpreadsheetML 合并区域。
     */
    public static final class SpreadsheetXmlMergeRegion {
        private final int startRow;
        private final int startColumn;
        private final int endRow;
        private final int endColumn;

        public SpreadsheetXmlMergeRegion(int startRow, int startColumn, int endRow, int endColumn) {
            this.startRow = startRow;
            this.startColumn = startColumn;
            this.endRow = endRow;
            this.endColumn = endColumn;
        }

        public int startRow() {
            return startRow;
        }

        public int startColumn() {
            return startColumn;
        }

        public int endRow() {
            return endRow;
        }

        public int endColumn() {
            return endColumn;
        }
    }
}
