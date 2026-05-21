package com.yss.valset.task.application.service.parseissue;

import com.yss.valset.task.application.command.parseissue.FileParseSourceSheetSaveCommand;
import com.yss.valset.task.application.command.parseissue.FileParseRuleSheetSaveCommand;
import com.yss.valset.task.application.command.parseissue.ProductMatchRuleSheetSaveCommand;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingExportDTO;
import com.yss.valset.task.application.dto.parseissue.FileParseSourceSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.FileParseRuleSheetRowDTO;
import com.yss.valset.task.application.dto.parseissue.ParseIssueHandlingSaveResultDTO;
import com.yss.valset.task.application.dto.parseissue.ProductMatchRuleSheetRowDTO;

import java.util.List;

/**
 * 解析问题处理配置维护服务。
 */
public interface ParseIssueHandlingAppService {

    List<FileParseSourceSheetRowDTO> listFileParseSources(String fileType,
                                                          String columnMap,
                                                          String columnName,
                                                          String status);

    ParseIssueHandlingExportDTO exportFileParseSources(String fileType,
                                                      String columnMap,
                                                      String columnName,
                                                      String status);

    ParseIssueHandlingSaveResultDTO saveFileParseSources(FileParseSourceSheetSaveCommand command);

    List<FileParseRuleSheetRowDTO> listFileParseRules(String fileScene,
                                                     String fileTypeName,
                                                     String regionName,
                                                     String columnMap,
                                                     String columnMapName,
                                                     String status);

    ParseIssueHandlingExportDTO exportFileParseRules(String fileScene,
                                                     String fileTypeName,
                                                     String regionName,
                                                     String columnMap,
                                                     String columnMapName,
                                                     String status);

    ParseIssueHandlingSaveResultDTO saveFileParseRules(FileParseRuleSheetSaveCommand command);

    List<ProductMatchRuleSheetRowDTO> listProductMatchRules(String pdCd,
                                                            String pdNm,
                                                            String orgNm,
                                                            String fileType,
                                                            String isValid);

    ParseIssueHandlingExportDTO exportProductMatchRules(String pdCd,
                                                       String pdNm,
                                                       String orgNm,
                                                       String fileType,
                                                       String isValid);

    ParseIssueHandlingSaveResultDTO saveProductMatchRules(ProductMatchRuleSheetSaveCommand command);
}
