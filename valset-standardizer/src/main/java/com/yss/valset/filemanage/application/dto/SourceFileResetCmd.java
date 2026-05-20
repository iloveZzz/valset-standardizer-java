package com.yss.valset.filemanage.application.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 源文件重新分析请求。
 */
@Data
public class SourceFileResetCmd implements java.io.Serializable {

    /** 需要重置的文件ID列表 */
    @NotEmpty(message = "重置异常文件id不能为空")
    private List<String> ids;
}
