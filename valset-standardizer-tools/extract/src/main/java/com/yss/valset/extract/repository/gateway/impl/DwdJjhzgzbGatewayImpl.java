package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.gateway.DwdJjhzgzbGateway;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.repository.entity.TrDwdJjhzgzbPO;
import com.yss.valset.extract.repository.mapper.TrDwdJjhzgzbRepository;
import com.yss.valset.extract.support.ProductBusinessFields;
import com.yss.valset.extract.support.JjhzgzbStandardizationSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * tr_spv_jjhzgzb 标准化落库网关实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DwdJjhzgzbGatewayImpl implements DwdJjhzgzbGateway {

    private final TrDwdJjhzgzbRepository repository;

    private final ProductBusinessFieldResolver productBusinessFieldResolver;

    private final DatabaseDialectSupport databaseDialectSupport;

    @Override
    public void saveStandardizedJjhzgzb(Long taskId, Long fileId, String sourceTp, String sourceSign, ParsedValuationData standardizedValuationData) {
        ProductBusinessFields productBusinessFields = productBusinessFieldResolver.resolve(fileId);
        List<TrDwdJjhzgzbPO> rows = JjhzgzbStandardizationSupport.buildRows(standardizedValuationData, sourceTp, sourceSign, productBusinessFields);
        if (rows.isEmpty()) {
            log.info("tr_spv_jjhzgzb 标准化结果为空，taskId={}, fileId={}, sourceTp={}", taskId, fileId, sourceTp);
            return;
        }
        Set<SnapshotKey> snapshotKeys = validateRows(taskId, fileId, rows);
        deleteExistingRows(snapshotKeys);
        insertRows(rows);
        TrDwdJjhzgzbPO firstRow = rows.get(0);
        log.info("tr_spv_jjhzgzb 标准化落地完成，taskId={}, fileId={}, sourceTp={}, rowCount={}, firstSubjectCd={}, firstBizDate={}",
                taskId,
                fileId,
                sourceTp,
                rows.size(),
                firstRow.getSubjectCd(),
                firstRow.getBizDate());
    }

    private Set<SnapshotKey> validateRows(Long taskId, Long fileId, List<TrDwdJjhzgzbPO> rows) {
        Set<SnapshotKey> snapshotKeys = new LinkedHashSet<>();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        int rowNumber = 0;
        for (TrDwdJjhzgzbPO row : rows) {
            rowNumber++;
            String pdCd = requireText(row == null ? null : row.getPdCd(), "PD_CD", taskId, fileId, rowNumber);
            String orgCd = requireText(row.getOrgCd(), "ORG_CD", taskId, fileId, rowNumber);
            String bizDate = requireText(row.getBizDate(), "BIZ_DATE", taskId, fileId, rowNumber);
            String subjectCd = requireText(row.getSubjectCd(), "SUBJECT_CD", taskId, fileId, rowNumber);
            Integer sn = requireSn(row.getSn(), taskId, fileId, rowNumber);
            row.setPdCd(pdCd);
            row.setOrgCd(orgCd);
            row.setBizDate(bizDate);
            row.setSubjectCd(subjectCd);
            row.setSn(sn);
            snapshotKeys.add(new SnapshotKey(pdCd, orgCd, bizDate));
            String uniqueKey = pdCd + "|" + orgCd + "|" + bizDate + "|" + subjectCd + "|" + sn;
            if (!uniqueKeys.add(uniqueKey)) {
                throw new IllegalStateException("tr_spv_jjhzgzb 标准落地存在重复业务键，taskId=" + taskId
                        + "，fileId=" + fileId
                        + "，pdCd=" + pdCd
                        + "，orgCd=" + orgCd
                        + "，bizDate=" + bizDate
                        + "，subjectCd=" + subjectCd
                        + "，sn=" + sn);
            }
        }
        return snapshotKeys;
    }

    private String requireText(String value, String fieldName, Long taskId, Long fileId, int rowNumber) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("tr_spv_jjhzgzb 标准落地缺少唯一键字段 " + fieldName
                    + "，taskId=" + taskId
                    + "，fileId=" + fileId
                    + "，rowNumber=" + rowNumber);
        }
        return value.trim();
    }

    private Integer requireSn(Integer value, Long taskId, Long fileId, int rowNumber) {
        if (value == null) {
            throw new IllegalStateException("tr_spv_jjhzgzb 标准落地缺少唯一键字段 SN"
                    + "，taskId=" + taskId
                    + "，fileId=" + fileId
                    + "，rowNumber=" + rowNumber);
        }
        return value;
    }

    private void deleteExistingRows(Set<SnapshotKey> snapshotKeys) {
        for (SnapshotKey snapshotKey : snapshotKeys) {
            repository.delete(Wrappers.lambdaQuery(TrDwdJjhzgzbPO.class)
                    .eq(TrDwdJjhzgzbPO::getPdCd, snapshotKey.pdCd)
                    .eq(TrDwdJjhzgzbPO::getOrgCd, snapshotKey.orgCd)
                    .eq(TrDwdJjhzgzbPO::getBizDate, snapshotKey.bizDate));
        }
    }

    private void insertRows(List<TrDwdJjhzgzbPO> rows) {
        if (databaseDialectSupport != null && databaseDialectSupport.isOracle()) {
            rows.forEach(repository::insert);
            return;
        }
        repository.insertBatchSomeColumn(rows);
    }

    private static final class SnapshotKey {
        private final String pdCd;
        private final String orgCd;
        private final String bizDate;

        private SnapshotKey(String pdCd, String orgCd, String bizDate) {
            this.pdCd = pdCd;
            this.orgCd = orgCd;
            this.bizDate = bizDate;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof SnapshotKey)) {
                return false;
            }
            SnapshotKey that = (SnapshotKey) object;
            return pdCd.equals(that.pdCd)
                    && orgCd.equals(that.orgCd)
                    && bizDate.equals(that.bizDate);
        }

        @Override
        public int hashCode() {
            int result = pdCd.hashCode();
            result = 31 * result + orgCd.hashCode();
            result = 31 * result + bizDate.hashCode();
            return result;
        }
    }
}
