package com.yss.valset.extract.support;

import com.yss.valset.common.support.ExcelParsingSupport;
import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.entity.TcAsIndexPO;
import com.yss.valset.extract.repository.entity.TrIndexPO;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 将 tr_spv_index 的 KV 指标行转换为 tc_as_index 宽表行。
 */
@Slf4j
public final class TcAsIndexWideRowSupport {

    private static final Map<String, BiConsumer<TcAsIndexPO, BigDecimal>> FIELD_SETTERS = buildFieldSetters();

    private TcAsIndexWideRowSupport() {
    }

    public static List<TcAsIndexPO> buildRows(List<TrIndexPO> indexRows, List<FileParseSourcePO> parseSources) {
        if (indexRows == null || indexRows.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Map<String, String> columnMapByName = buildColumnMapByName(parseSources);
        if (columnMapByName.isEmpty()) {
            log.info("tc_as_index 转换跳过：T_FILE_PARSE_SOURCE 未找到启用的指标来源映射");
            return java.util.Collections.emptyList();
        }
        LocalDateTime timeStamp = LocalDateTime.now();
        Map<GroupKey, TcAsIndexPO> wideRows = new LinkedHashMap<>();
        for (TrIndexPO indexRow : indexRows) {
            if (indexRow == null) {
                continue;
            }
            String columnMap = columnMapByName.get(trimToNull(indexRow.getIndxNm()));
            if (columnMap == null) {
                log.debug("tc_as_index 指标跳过：未找到来源映射，indxNm={}", indexRow.getIndxNm());
                continue;
            }
            BiConsumer<TcAsIndexPO, BigDecimal> setter = FIELD_SETTERS.get(columnMap);
            if (setter == null) {
                log.warn("tc_as_index 指标跳过：COLUMN_MAP 不属于标准资产指标字段，indxNm={}, columnMap={}",
                        indexRow.getIndxNm(), columnMap);
                continue;
            }
            BigDecimal value = ExcelParsingSupport.normalizeNumber(indexRow.getIndxValu());
            if (value == null) {
                log.debug("tc_as_index 指标跳过：指标值非数值，indxNm={}, indxValu={}",
                        indexRow.getIndxNm(), indexRow.getIndxValu());
                continue;
            }
            GroupKey groupKey = new GroupKey(indexRow.getPdCd(), indexRow.getOrgCd(), indexRow.getBizDate());
            TcAsIndexPO wideRow = wideRows.computeIfAbsent(groupKey, key -> buildBaseRow(indexRow, timeStamp));
            setter.accept(wideRow, value);
        }
        return new ArrayList<>(wideRows.values());
    }

    private static Map<String, String> buildColumnMapByName(List<FileParseSourcePO> parseSources) {
        if (parseSources == null || parseSources.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Map<String, String> columnMapByName = new LinkedHashMap<>();
        for (FileParseSourcePO parseSource : parseSources) {
            if (parseSource == null || !Boolean.TRUE.equals(parseSource.getStatus())) {
                continue;
            }
            String columnName = trimToNull(parseSource.getColumnName());
            String columnMap = trimToNull(parseSource.getColumnMap());
            if (columnName == null || columnMap == null) {
                continue;
            }
            columnMapByName.put(columnName, columnMap);
        }
        return columnMapByName;
    }

    private static TcAsIndexPO buildBaseRow(TrIndexPO indexRow, LocalDateTime timeStamp) {
        TcAsIndexPO wideRow = new TcAsIndexPO();
        wideRow.setPdCd(indexRow.getPdCd());
        wideRow.setOrgCd(indexRow.getOrgCd());
        wideRow.setBizDate(indexRow.getBizDate());
        wideRow.setTimeStamp(timeStamp);
        return wideRow;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, BiConsumer<TcAsIndexPO, BigDecimal>> buildFieldSetters() {
        Map<String, BiConsumer<TcAsIndexPO, BigDecimal>> setters = new LinkedHashMap<>();
        setters.put("paid_capital", TcAsIndexPO::setPaidCapital);
        setters.put("total_assets", TcAsIndexPO::setTotalAssets);
        setters.put("total_liabi", TcAsIndexPO::setTotalLiabi);
        setters.put("asset_value", TcAsIndexPO::setAssetValue);
        setters.put("avg_nav", TcAsIndexPO::setAvgNav);
        setters.put("acc_net", TcAsIndexPO::setAccNet);
        setters.put("ten_sou_yield", TcAsIndexPO::setTenSouYield);
        setters.put("seven_annu_yield", TcAsIndexPO::setSevenAnnuYield);
        setters.put("today_annu_yield", TcAsIndexPO::setTodayAnnuYield);
        setters.put("yield", TcAsIndexPO::setYield);
        setters.put("deviation", TcAsIndexPO::setDeviation);
        setters.put("deviation_amt", TcAsIndexPO::setDeviationAmt);
        setters.put("total_assets_cb", TcAsIndexPO::setTotalAssetsCb);
        setters.put("total_liabi_cb", TcAsIndexPO::setTotalLiabiCb);
        setters.put("asset_value_cb", TcAsIndexPO::setAssetValueCb);
        setters.put("total_assets_cb_y", TcAsIndexPO::setTotalAssetsCbY);
        setters.put("total_liabi_cb_y", TcAsIndexPO::setTotalLiabiCbY);
        setters.put("asset_value_cb_y", TcAsIndexPO::setAssetValueCbY);
        setters.put("total_assets_y", TcAsIndexPO::setTotalAssetsY);
        setters.put("total_liabi_y", TcAsIndexPO::setTotalLiabiY);
        setters.put("asset_value_y", TcAsIndexPO::setAssetValueY);
        setters.put("paid_capital_cb", TcAsIndexPO::setPaidCapitalCb);
        return setters;
    }

    private static final class GroupKey {
        private final String pdCd;
        private final String orgCd;
        private final String bizDate;

        private GroupKey(String pdCd, String orgCd, String bizDate) {
            this.pdCd = pdCd;
            this.orgCd = orgCd;
            this.bizDate = bizDate;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof GroupKey)) {
                return false;
            }
            GroupKey groupKey = (GroupKey) object;
            return pdCd.equals(groupKey.pdCd)
                    && orgCd.equals(groupKey.orgCd)
                    && bizDate.equals(groupKey.bizDate);
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
