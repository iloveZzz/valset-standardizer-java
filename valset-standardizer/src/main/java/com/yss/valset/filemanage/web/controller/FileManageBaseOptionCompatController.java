package com.yss.valset.filemanage.web.controller;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.valset.filemanage.application.dto.DataDictVO;
import com.yss.valset.filemanage.application.dto.OrgBasicInfoVO;
import com.yss.valset.filemanage.application.service.FileManageBaseOptionCompatAppService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件收取管理基础选项前端兼容接口。
 */
@RestController
@RequiredArgsConstructor
public class FileManageBaseOptionCompatController {

    private final FileManageBaseOptionCompatAppService fileManageBaseOptionCompatAppService;

    @GetMapping("/data/dict/list")
    @Operation(summary = "数据字典项-列表查询")
    public MultiResult<DataDictVO> listDict(@RequestParam("classCode") String classCode) {
        return MultiResult.of(fileManageBaseOptionCompatAppService.listDict(classCode));
    }

    @GetMapping("/org/bsc/info/list")
    @Operation(summary = "查询返回所有已审核组织基础信息列表")
    public MultiResult<OrgBasicInfoVO> listOrganizations() {
        return MultiResult.of(fileManageBaseOptionCompatAppService.listOrganizations());
    }
}
