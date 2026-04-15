package com.yss.subjectmatch.analysis.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
import com.yss.subjectmatch.extract.repository.entity.ValuationDataPO;
import com.yss.subjectmatch.extract.repository.mapper.UnValuationDataMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库非标准化估值表解析器。
 */
@Component
public class DbValuationDataParser implements ValuationDataParser {

    private final UnValuationDataMapper unValuationDataMapper;

    public DbValuationDataParser(UnValuationDataMapper unValuationDataMapper) {
        this.unValuationDataMapper = unValuationDataMapper;
    }

    @Override
    public ParsedValuationData parse(DataSourceConfig config) {
        List<SubjectRecord> subjects = new ArrayList<>();
        List<MetricRecord> metrics = new ArrayList<>();

        try {
            QueryWrapper<ValuationDataPO> queryWrapper = new QueryWrapper<>();
            List<ValuationDataPO> dbRecords = unValuationDataMapper.selectList(queryWrapper);

            if (dbRecords != null) {
                for (ValuationDataPO po : dbRecords) {
                    SubjectRecord subject = new SubjectRecord();
                    subject.setSubjectCode(po.getSubjectCode());
                    subject.setSubjectName(po.getSubjectName());
                    subjects.add(subject);
                }
            }

            return ParsedValuationData.builder()
                    .workbookPath("db://t_ods_valuation_data")
                    .subjects(subjects)
                    .metrics(metrics)
                    .title("DB Source: t_ods_valuation_data")
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse DB source: t_ods_valuation_data", e);
        }
    }
}
