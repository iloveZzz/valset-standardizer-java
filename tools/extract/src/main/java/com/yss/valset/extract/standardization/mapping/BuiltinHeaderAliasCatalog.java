package com.yss.valset.extract.standardization.mapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外部估值表头内置别名白名单。
 * <p>
 * 仅作为规则库的补充兜底，优先级低于配置化规则。
 * </p>
 */
public final class BuiltinHeaderAliasCatalog {

    private static final Map<String, String> ALIAS_TO_STANDARD_CODE;

    static {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("科目代码", "subject_cd");
        mapping.put("科目名称", "subject_nm");
        mapping.put("数量", "n_hldamt");
        mapping.put("币种", "ccy_cd");
        mapping.put("汇率", "n_valrate");
        mapping.put("单位成本", "n_price_cost");
        mapping.put("证券估值行情", "n_valprice");
        mapping.put("原币持仓成本", "n_hldcst");
        mapping.put("本币持仓成本", "n_hldcst_locl");
        mapping.put("成本|本币", "n_hldcst_locl");
        mapping.put("原币持仓市值", "n_hldmkv");
        mapping.put("本币持仓市值", "n_hldmkv_locl");
        mapping.put("市值|本币", "n_hldmkv_locl");
        mapping.put("原币证券估值增值", "n_hldvva");
        mapping.put("本币证券估值增值", "n_hldvva_l");
        mapping.put("估值增值|本币", "n_hldvva_l");
        mapping.put("行情|本币", "n_valprice");
        mapping.put("成本占比", "n_cb_jz_bl");
        mapping.put("市值占比", "n_sz_jz_bl");
        mapping.put("资产占比", "n_zc_bl");
        mapping.put("停牌信息", "susp_info");
        mapping.put("估值权益", "valuat_equity");
        mapping.put("权益信息|本币", "valuat_equity");
        ALIAS_TO_STANDARD_CODE = Map.copyOf(mapping);
    }

    private BuiltinHeaderAliasCatalog() {
    }

    public static HeaderMappingCandidate matchByContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String candidateText = canonicalize(text);
        String bestAlias = null;
        String bestCode = null;
        for (Map.Entry<String, String> entry : ALIAS_TO_STANDARD_CODE.entrySet()) {
            String alias = entry.getKey();
            if (alias == null || alias.isBlank() || alias.length() < 2) {
                continue;
            }
            String normalizedAlias = canonicalize(alias);
            if (!candidateText.contains(normalizedAlias)) {
                continue;
            }
            if (bestAlias == null || normalizedAlias.length() > bestAlias.length()) {
                bestAlias = normalizedAlias;
                bestCode = entry.getValue();
            }
        }
        if (bestCode == null || bestCode.isBlank()) {
            return null;
        }
        return new HeaderMappingCandidate(null, null, bestCode);
    }

    private static String canonicalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replace('｜', '|')
                .replace('¦', '|')
                .replace('／', '/')
                .replaceAll("\\s+", "");
    }
}
