package com.yss.valset.transfer.domain.rule;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 脚本规则可用的基础函数。
 */
public class TransferRuleFunctions {

    public boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        return source.toLowerCase().contains(keyword.toLowerCase());
    }

    public boolean matchesRegex(String source, String regex) {
        if (source == null || regex == null) {
            return false;
        }
        return Pattern.compile(regex).matcher(source).matches();
    }

    public boolean isExcel(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    public boolean isCompressed(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z");
    }

    public boolean senderInWhitelist(String sender, Collection<String> whitelist) {
        if (sender == null || whitelist == null) {
            return false;
        }
        return whitelist.stream().filter(Objects::nonNull).anyMatch(item -> sender.equalsIgnoreCase(item));
    }
}
