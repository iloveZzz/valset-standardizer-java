package com.yss.valset.task.application.command.parseissue;

import com.yss.valset.task.application.dto.parseissue.FileParseSourceSheetRowDTO;
import lombok.Data;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析字段映射 Sheet 保存命令。
 */
@Data
public class FileParseSourceSheetSaveCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    private List<FileParseSourceSheetRowDTO> rows = new ArrayList<>();

    private List<String> originalIds = new ArrayList<>();
}
