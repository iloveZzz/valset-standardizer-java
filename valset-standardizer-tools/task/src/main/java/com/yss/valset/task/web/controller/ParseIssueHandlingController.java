package com.yss.valset.task.web.controller;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingExportDTO;
import com.yss.valset.task.application.command.parseissue.FileParseSourceSheetSaveCommand;
import com.yss.valset.task.application.command.parseissue.FileParseRuleSheetSaveCommand;
import com.yss.valset.task.application.command.parseissue.ProductMatchRuleSheetSaveCommand;
import com.yss.valset.task.application.dto.parseissue.FileParseSourceSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.FileParseRuleSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingSaveResultDTO;
import com.yss.valset.task.application.dto.parseissue.ProductMatchRuleSheetRowDTO;
import com.yss.valset.task.application.service.parseissue.ParseIssueHandlingAppService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;

/**
 * 解析问题处理配置维护接口。
 */
@RestController
@RequestMapping("/parse-issue-handling")
@RequiredArgsConstructor
public class ParseIssueHandlingController {

    private final ParseIssueHandlingAppService parseIssueHandlingAppService;

    @GetMapping("/file-parse-sources")
    @Operation(summary = "查询解析字段映射表")
    public MultiResult<FileParseSourceSheetRowDTO> listFileParseSources(
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "columnMap", required = false) String columnMap,
            @RequestParam(value = "columnName", required = false) String columnName,
            @RequestParam(value = "status", required = false) String status) {
        return MultiResult.of(parseIssueHandlingAppService.listFileParseSources(fileType, columnMap, columnName, status));
    }

    @GetMapping(value = "/file-parse-sources/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "导出外部列指标映射")
    public ResponseEntity<Resource> exportFileParseSources(
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "columnMap", required = false) String columnMap,
            @RequestParam(value = "columnName", required = false) String columnName,
            @RequestParam(value = "status", required = false) String status) {
        return buildExportResponse(parseIssueHandlingAppService.exportFileParseSources(fileType, columnMap, columnName, status));
    }

    @PutMapping("/file-parse-sources")
    @Operation(summary = "保存解析字段映射表")
    public SingleResult<ParseIssueHandlingSaveResultDTO> saveFileParseSources(
            @Valid @RequestBody FileParseSourceSheetSaveCommand command) {
        return SingleResult.of(parseIssueHandlingAppService.saveFileParseSources(command));
    }

    @GetMapping("/file-parse-rules")
    @Operation(summary = "查询标准列指标映射")
    public MultiResult<FileParseRuleSheetRowDTO> listFileParseRules(
            @RequestParam(value = "fileScene", required = false) String fileScene,
            @RequestParam(value = "fileTypeName", required = false) String fileTypeName,
            @RequestParam(value = "regionName", required = false) String regionName,
            @RequestParam(value = "columnMap", required = false) String columnMap,
            @RequestParam(value = "columnMapName", required = false) String columnMapName,
            @RequestParam(value = "status", required = false) String status) {
        return MultiResult.of(parseIssueHandlingAppService.listFileParseRules(
                fileScene, fileTypeName, regionName, columnMap, columnMapName, status));
    }

    @GetMapping(value = "/file-parse-rules/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "导出标准列指标映射")
    public ResponseEntity<Resource> exportFileParseRules(
            @RequestParam(value = "fileScene", required = false) String fileScene,
            @RequestParam(value = "fileTypeName", required = false) String fileTypeName,
            @RequestParam(value = "regionName", required = false) String regionName,
            @RequestParam(value = "columnMap", required = false) String columnMap,
            @RequestParam(value = "columnMapName", required = false) String columnMapName,
            @RequestParam(value = "status", required = false) String status) {
        return buildExportResponse(parseIssueHandlingAppService.exportFileParseRules(
                fileScene, fileTypeName, regionName, columnMap, columnMapName, status));
    }

    @PutMapping("/file-parse-rules")
    @Operation(summary = "保存标准列指标映射")
    public SingleResult<ParseIssueHandlingSaveResultDTO> saveFileParseRules(
            @Valid @RequestBody FileParseRuleSheetSaveCommand command) {
        return SingleResult.of(parseIssueHandlingAppService.saveFileParseRules(command));
    }

    @GetMapping("/product-match-rules")
    @Operation(summary = "查询产品识别规则配置表")
    public MultiResult<ProductMatchRuleSheetRowDTO> listProductMatchRules(
            @RequestParam(value = "pdCd", required = false) String pdCd,
            @RequestParam(value = "pdNm", required = false) String pdNm,
            @RequestParam(value = "orgNm", required = false) String orgNm,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "isValid", required = false) String isValid) {
        return MultiResult.of(parseIssueHandlingAppService.listProductMatchRules(pdCd, pdNm, orgNm, fileType, isValid));
    }

    @GetMapping(value = "/product-match-rules/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "导出产品识别规则配置表")
    public ResponseEntity<Resource> exportProductMatchRules(
            @RequestParam(value = "pdCd", required = false) String pdCd,
            @RequestParam(value = "pdNm", required = false) String pdNm,
            @RequestParam(value = "orgNm", required = false) String orgNm,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "isValid", required = false) String isValid) {
        return buildExportResponse(parseIssueHandlingAppService.exportProductMatchRules(pdCd, pdNm, orgNm, fileType, isValid));
    }

    @PutMapping("/product-match-rules")
    @Operation(summary = "保存产品识别规则配置表")
    public SingleResult<ParseIssueHandlingSaveResultDTO> saveProductMatchRules(
            @Valid @RequestBody ProductMatchRuleSheetSaveCommand command) {
        return SingleResult.of(parseIssueHandlingAppService.saveProductMatchRules(command));
    }

    private ResponseEntity<Resource> buildExportResponse(ParseIssueHandlingExportDTO export) {
        ByteArrayResource resource = new ByteArrayResource(export.getContent());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(export.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(export.getContent() == null ? 0 : export.getContent().length)
                .body(resource);
    }
}
