package com.yss.valset.filemanage.application.service;

import com.yss.valset.filemanage.application.dto.DataDictVO;
import com.yss.valset.filemanage.application.dto.OrgBasicInfoVO;

import java.util.List;

/**
 * 文件收取管理基础下拉兼容服务。
 */
public interface FileManageBaseOptionCompatAppService {

    List<DataDictVO> listDict(String classCode);

    List<OrgBasicInfoVO> listOrganizations();
}
