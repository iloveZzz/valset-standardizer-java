package com.yss.valset.filemanage.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.filemanage.application.dto.FileReceiveVO;
import com.yss.valset.filemanage.application.dto.FileStateGroupVO;
import com.yss.valset.filemanage.application.dto.SourceFileManagePage;
import com.yss.valset.filemanage.application.dto.SourceFileManageQuery;
import com.yss.valset.filemanage.application.dto.SourceFileManageVO;
import com.yss.valset.filemanage.application.dto.SourceFileResetCmd;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件收取管理前端兼容应用服务。
 */
public interface FileManageCompatAppService {

    PageResult<SourceFileManageVO> page(SourceFileManagePage query);

    List<FileStateGroupVO> stateGroups(SourceFileManageQuery query);

    FileReceiveVO uploadBatch(List<MultipartFile> files);

    FileReceiveVO ingestByDate(String startDate);

    void reset(SourceFileResetCmd command);

    ResponseEntity<Resource> download(String fileState, String id);

    ResponseEntity<Resource> exportList(SourceFileManageQuery query);
}
