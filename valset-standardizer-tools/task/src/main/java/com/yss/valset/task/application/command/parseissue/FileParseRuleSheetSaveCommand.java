package com.yss.valset.task.application.command.parseissue;

import com.yss.valset.task.application.dto.parseissue.FileParseRuleSheetRowDTO;
import lombok.Data;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析标准规则 Sheet 保存命令。
 */
@Data
public class FileParseRuleSheetSaveCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    private List<FileParseRuleSheetRowDTO> rows = new ArrayList<>();

    private List<String> originalIds = new ArrayList<>();
}
