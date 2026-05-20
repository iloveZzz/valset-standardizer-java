package com.yss.valset.qlexpress.domain.runtime;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 所有 QLExpress 场景共享的受控函数对象。
 */
@Component
public class QlexpressCommonFunctionFacade {    

    public boolean hasText(Object value) {
        return value != null && !String.valueOf(value).trim().isEmpty();
    }

    public boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public boolean matchesRegex(Object source, Object regex) {
        if (source == null || regex == null) {
            return false;
        }
        String sourceText = String.valueOf(source).trim();
        String regexText = String.valueOf(regex).trim();
        if (sourceText.isEmpty() || regexText.isEmpty()) {
            return false;
        }
        return Pattern.compile(regexText).matcher(sourceText).matches();
    }

    public boolean matchesRegex(String source, String regex) {
        return matchesRegex((Object) source, regex);
    }
}
