package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.gateway.TrIndexGateway;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.extract.repository.entity.TrIndexPO;
import com.yss.valset.extract.repository.mapper.TrIndexRepository;
import com.yss.valset.extract.support.ProductBusinessFields;
import com.yss.valset.extract.support.TrIndexStandardizationSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * tr_spv_index 标准化落库网关实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TrIndexGatewayImpl implements TrIndexGateway {

    private final TrIndexRepository repository;

    private final ProductBusinessFieldResolver productBusinessFieldResolver;

    private final DatabaseDialectSupport databaseDialectSupport;

    @Override
    public void saveStandardizedIndex(Long taskId, Long fileId, String sourceTp, String sourceSign, ParsedValuationData standardizedValuationData) {
        ProductBusinessFields productBusinessFields = productBusinessFieldResolver.resolve(fileId);
        List<TrIndexPO> rows = TrIndexStandardizationSupport.buildRows(standardizedValuationData, sourceTp, sourceSign, productBusinessFields);
        if (rows.isEmpty()) {
            log.info("tr_spv_index 标准化结果为空，taskId={}, fileId={}, sourceTp={}", taskId, fileId, sourceTp);
            return;
        }
        Set<SnapshotKey> snapshotKeys = validateRows(taskId, fileId, rows);
        deleteExistingRows(snapshotKeys);
        insertRows(rows);
        TrIndexPO firstRow = rows.get(0);
        log.info("tr_spv_index 标准化落地完成，taskId={}, fileId={}, sourceTp={}, rowCount={}, firstIndxNm={}, firstBizDate={}",
                taskId,
                fileId,
                sourceTp,
                rows.size(),
                firstRow.getIndxNm(),
                firstRow.getBizDate());
    }

    private Set<SnapshotKey> validateRows(Long taskId, Long fileId, List<TrIndexPO> rows) {
        Set<SnapshotKey> snapshotKeys = new LinkedHashSet<>();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        int rowNumber = 0;
        for (TrIndexPO row : rows) {
            rowNumber++;
            String pdCd = requireText(row == null ? null : row.getPdCd(), "PD_CD", taskId, fileId, rowNumber);
            String orgCd = requireText(row.getOrgCd(), "ORG_CD", taskId, fileId, rowNumber);
            String bizDate = requireText(row.getBizDate(), "BIZ_DATE", taskId, fileId, rowNumber);
            String indxNm = requireText(row.getIndxNm(), "INDX_NM", taskId, fileId, rowNumber);
            Integer sn = requireSn(row.getSn(), taskId, fileId, rowNumber, indxNm);
            row.setPdCd(pdCd);
            row.setOrgCd(orgCd);
            row.setBizDate(bizDate);
            row.setIndxNm(indxNm);
            snapshotKeys.add(new SnapshotKey(pdCd, orgCd, bizDate));
            String uniqueKey = pdCd + "|" + orgCd + "|" + bizDate + "|" + indxNm + "|" + sn;
            if (!uniqueKeys.add(uniqueKey)) {
                throw new IllegalStateException("tr_spv_index 标准落地存在重复业务键，taskId=" + taskId
                        + "，fileId=" + fileId
                        + "，pdCd=" + pdCd
                        + "，orgCd=" + orgCd
                        + "，bizDate=" + bizDate
                        + "，indxNm=" + indxNm
                        + "，sn=" + sn);
            }
        }
        return snapshotKeys;
    }

    private Integer requireSn(Integer sn, Long taskId, Long fileId, int rowNumber, String indxNm) {
        if (sn == null) {
            throw new IllegalStateException("tr_spv_index 标准落地缺少唯一键字段 SN"
                    + "，taskId=" + taskId
                    + "，fileId=" + fileId
                    + "，rowNumber=" + rowNumber
                    + "，indxNm=" + indxNm);
        }
        return sn;
    }

    private String requireText(String value, String fieldName, Long taskId, Long fileId, int rowNumber) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("tr_spv_index 标准落地缺少唯一键字段 " + fieldName
                    + "，taskId=" + taskId
                    + "，fileId=" + fileId
                    + "，rowNumber=" + rowNumber);
        }
        return value.trim();
    }

    private void deleteExistingRows(Set<SnapshotKey> snapshotKeys) {
        for (SnapshotKey snapshotKey : snapshotKeys) {
            repository.delete(Wrappers.lambdaQuery(TrIndexPO.class)
                    .eq(TrIndexPO::getPdCd, snapshotKey.pdCd)
                    .eq(TrIndexPO::getOrgCd, snapshotKey.orgCd)
                    .eq(TrIndexPO::getBizDate, snapshotKey.bizDate));
        }
    }

    private void insertRows(List<TrIndexPO> rows) {
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
