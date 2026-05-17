package com.yss.valset.filemanage.application.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.filemanage.application.dto.DataDictVO;
import com.yss.valset.filemanage.application.dto.OrgBasicInfoVO;
import com.yss.valset.filemanage.application.service.FileManageBaseOptionCompatAppService;
import com.yss.valset.transfer.infrastructure.entity.TransferSourcePO;
import com.yss.valset.transfer.infrastructure.mapper.TransferSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认文件收取管理基础下拉兼容服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultFileManageBaseOptionCompatAppService implements FileManageBaseOptionCompatAppService {

    private final TransferSourceRepository transferSourceRepository;

    @Override
    public List<DataDictVO> listDict(String classCode) {
        if ("PD_TYPE".equalsIgnoreCase(classCode)) {
            return Arrays.asList(
                    dict("PD_TYPE", "产品类型", "SPV", "委外产品"),
                    dict("PD_TYPE", "产品类型", "FIM", "理财产品")
            );
        }
        if ("FILE_STATE".equalsIgnoreCase(classCode)) {
            return Arrays.asList(
                    dict("FILE_STATE", "文件状态", "未匹配", "未匹配"),
                    dict("FILE_STATE", "文件状态", "待分析", "待分析"),
                    dict("FILE_STATE", "文件状态", "已分析", "已分析"),
                    dict("FILE_STATE", "文件状态", "分析异常", "分析异常"),
                    dict("FILE_STATE", "文件状态", "已失效", "已失效")
            );
        }
        if ("FILE_RECEIVE_WAY".equalsIgnoreCase(classCode)) {
            return Arrays.asList(
                    dict("FILE_RECEIVE_WAY", "收取方式", "HTTP", "手动上传"),
                    dict("FILE_RECEIVE_WAY", "收取方式", "EMAIL", "邮件"),
                    dict("FILE_RECEIVE_WAY", "收取方式", "SFTP", "SFTP"),
                    dict("FILE_RECEIVE_WAY", "收取方式", "S3", "S3"),
                    dict("FILE_RECEIVE_WAY", "收取方式", "LOCAL_DIR", "本地目录")
            );
        }
        return new ArrayList<>();
    }

    @Override
    public List<OrgBasicInfoVO> listOrganizations() {
        return transferSourceRepository.selectList(
                        Wrappers.lambdaQuery(TransferSourcePO.class)
                                .eq(TransferSourcePO::getEnabled, Boolean.TRUE)
                                .orderByAsc(TransferSourcePO::getSourceCode)
                )
                .stream()
                .filter(source -> StringUtils.hasText(source.getSourceCode()))
                .map(source -> OrgBasicInfoVO.builder()
                        .id(source.getSourceId())
                        .orgCd(source.getSourceCode())
                        .orgFullNm(StringUtils.hasText(source.getSourceName()) ? source.getSourceName() : source.getSourceCode())
                        .orgAbbrNm(source.getSourceCode())
                        .isAudt(1)
                        .isDel(0)
                        .build())
                .collect(Collectors.toList());
    }

    private DataDictVO dict(String classCode, String className, String value, String name) {
        return DataDictVO.builder()
                .classCode(classCode)
                .className(className)
                .dictValue(value)
                .dictName(name)
                .systemCode("VALSET")
                .build();
    }
}
