package com.yss.valset.filemanage.web.controller;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.filemanage.application.dto.FileReceiveVO;
import com.yss.valset.filemanage.application.dto.FileStateGroupVO;
import com.yss.valset.filemanage.application.dto.SourceFileManagePage;
import com.yss.valset.filemanage.application.dto.SourceFileManageQuery;
import com.yss.valset.filemanage.application.dto.SourceFileManageVO;
import com.yss.valset.filemanage.application.dto.SourceFileResetCmd;
import com.yss.valset.filemanage.application.service.FileManageCompatAppService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

/**
 * 文件收取管理前端兼容接口。
 */
@RestController
@RequiredArgsConstructor
public class FileManageCompatController {

    private final FileManageCompatAppService fileManageCompatAppService;

    @PostMapping("/file/manage/page")
    @Operation(summary = "源文件信息管理-分页查询", description = "兼容前端文件收取管理页面，数据来自分拣对象、标签和解析队列。")
    public PageResult<SourceFileManageVO> pageFileManage(@RequestBody(required = false) SourceFileManagePage query) {
        return fileManageCompatAppService.page(query);
    }

    @PostMapping("/file/manage/state/group")
    @Operation(summary = "获取源文件管理状态分组信息")
    public MultiResult<FileStateGroupVO> fileManageStateGroup(@RequestBody(required = false) SourceFileManageQuery query) {
        return MultiResult.of(fileManageCompatAppService.stateGroups(query));
    }

    @GetMapping("/ingest/file/date")
    @Operation(summary = "收取指定日期的估值文件", description = "兼容前端入口，内部触发现有来源手动刷新/收取任务。")
    public SingleResult<FileReceiveVO> ingestValuationFileByDate(@RequestParam("startDate") String startDate) {
        return SingleResult.of(fileManageCompatAppService.ingestByDate(startDate));
    }

    @PutMapping("/exception/file/reset")
    @Operation(summary = "文件重新处理")
    public SingleResult<Boolean> resetSourceFile(@Valid @RequestBody SourceFileResetCmd command) {
        fileManageCompatAppService.reset(command);
        return SingleResult.of(Boolean.TRUE);
    }

    @PostMapping("/file/manage/export/list")
    @Operation(summary = "源文件批量下载")
    public ResponseEntity<Resource> sourceFileListExport(@RequestBody(required = false) SourceFileManageQuery query) {
        return fileManageCompatAppService.exportList(query);
    }

    @GetMapping("/file/manage/export/{fileState}")
    @Operation(summary = "源文件下载")
    public ResponseEntity<Resource> sourceFileExport(@PathVariable String fileState, @RequestParam("id") String id) {
        return fileManageCompatAppService.download(fileState, id);
    }

    @PostMapping("/file/manage/upload/batch")
    @Operation(summary = "源文件手动上传-批量", description = "兼容前端入口，内部复用分拣路由 HTTP 来源手动上传能力。")
    public SingleResult<FileReceiveVO> uploadSourceFiles(@RequestPart("files") List<MultipartFile> files) {
        return SingleResult.of(fileManageCompatAppService.uploadBatch(files));
    }
}
