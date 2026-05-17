package com.yss.valset.transfer.infrastructure.source.email;

import javax.mail.Message;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RFC 2047 邮件头解码工具，兼容部分邮件系统把 UTF-8 字节错误切分到多个 encoded-word 的情况。
 */
final class EmailMimeHeaderDecoder {

    private static final Pattern ENCODED_WORD_PATTERN = Pattern.compile("=\\?([^?\\s]+)\\?([bBqQ])\\?([^?]*)\\?=");

    private EmailMimeHeaderDecoder() {
    }

    static String decodeSubject(Message message) {
        if (message == null) {
            return null;
        }
        String standardSubject = null;
        try {
            standardSubject = message.getSubject();
            if (isHealthyDecodedText(standardSubject)) {
                return standardSubject;
            }
        } catch (Exception ignored) {
            // 继续读取原始 Subject 头做兼容解码。
        }
        try {
            String rawSubject = rawHeader(message, "Subject");
            if (!isBlank(rawSubject)) {
                String decoded = decodeHeader(rawSubject);
                if (isHealthyDecodedText(decoded) || isBlank(standardSubject)) {
                    return decoded;
                }
            }
        } catch (Exception ignored) {
            // 继续使用 JavaMail 的标准 subject 读取兜底。
        }
        return standardSubject;
    }

    private static String rawHeader(Message message, String name) throws Exception {
        String[] headers = message.getHeader(name);
        if (headers == null || headers.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String header : headers) {
            if (header == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\r\n ");
            }
            builder.append(header);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    static String decodeHeader(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        String normalized = normalizeHeaderValue(value);
        String standard = null;
        try {
            standard = MimeUtility.decodeText(normalized);
            if (isHealthyDecodedText(standard)) {
                return standard;
            }
        } catch (Exception ignored) {
            // 继续走兼容解码。
        }
        String unfolded = unfold(normalized);
        if (!containsEncodedWord(unfolded)) {
            return unfolded;
        }
        String decoded = decodeEncodedWords(unfolded);
        return bestCandidate(standard, decoded);
    }

    private static String decodeEncodedWords(String value) {
        StringBuilder output = new StringBuilder();
        Matcher matcher = ENCODED_WORD_PATTERN.matcher(value);
        int index = 0;
        while (matcher.find(index)) {
            output.append(value, index, matcher.start());
            List<EncodedWord> words = new ArrayList<>();
            words.add(toEncodedWord(matcher));
            int groupEnd = matcher.end();
            int nextIndex = matcher.end();
            while (true) {
                Matcher next = ENCODED_WORD_PATTERN.matcher(value);
                if (!next.find(nextIndex) || !isWhitespaceOnly(value, nextIndex, next.start())) {
                    break;
                }
                words.add(toEncodedWord(next));
                groupEnd = next.end();
                nextIndex = next.end();
            }
            output.append(decodeAdjacentWords(words));
            index = groupEnd;
        }
        output.append(value.substring(index));
        return output.toString();
    }

    private static String decodeAdjacentWords(List<EncodedWord> words) {
        StringBuilder decoded = new StringBuilder();
        int index = 0;
        while (index < words.size()) {
            EncodedWord first = words.get(index);
            int runEnd = index + 1;
            while (runEnd < words.size() && first.sameEncoding(words.get(runEnd))) {
                runEnd++;
            }
            decoded.append(decodeRun(words.subList(index, runEnd)));
            index = runEnd;
        }
        return decoded.toString();
    }

    private static String decodeRun(List<EncodedWord> words) {
        if (words == null || words.isEmpty()) {
            return "";
        }
        EncodedWord first = words.get(0);
        try {
            byte[] bytes = first.isBase64() ? decodeBase64Run(words) : decodeQRun(words);
            return new String(bytes, toCharset(first.charset));
        } catch (Exception ignored) {
            StringBuilder fallback = new StringBuilder();
            for (EncodedWord word : words) {
                fallback.append(decodeSingleWord(word));
            }
            return fallback.toString();
        }
    }

    private static byte[] decodeBase64Run(List<EncodedWord> words) throws Exception {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (EncodedWord word : words) {
                output.write(decodeBase64(cleanBase64(word.encodedText)));
            }
            return output.toByteArray();
        } catch (Exception ignored) {
            // 部分非标准片段自身 padding 错误，降级为整体拼接后解码。
        }
        StringBuilder joined = new StringBuilder();
        for (EncodedWord word : words) {
            String cleaned = cleanBase64(word.encodedText);
            joined.append(cleaned);
        }
        try {
            return decodeBase64(joined.toString());
        } catch (Exception ignored) {
            return decodeBase64(joined.toString().replace("=", ""));
        }
    }

    private static byte[] decodeQRun(List<EncodedWord> words) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (EncodedWord word : words) {
            byte[] bytes = decodeQEncoded(word.encodedText);
            output.write(bytes, 0, bytes.length);
        }
        return output.toByteArray();
    }

    private static String decodeSingleWord(EncodedWord word) {
        try {
            return MimeUtility.decodeText(word.original);
        } catch (Exception ignored) {
            return word.original;
        }
    }

    private static byte[] decodeBase64(String value) {
        String cleaned = cleanBase64(value);
        int remainder = cleaned.length() % 4;
        if (remainder == 2) {
            cleaned = cleaned + "==";
        } else if (remainder == 3) {
            cleaned = cleaned + "=";
        }
        return Base64.getMimeDecoder().decode(cleaned);
    }

    private static byte[] decodeQEncoded(String value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '_') {
                output.write(' ');
                continue;
            }
            if (current == '=' && index + 2 < value.length()) {
                int high = Character.digit(value.charAt(index + 1), 16);
                int low = Character.digit(value.charAt(index + 2), 16);
                if (high >= 0 && low >= 0) {
                    output.write((high << 4) + low);
                    index += 2;
                    continue;
                }
            }
            output.write((byte) current);
        }
        return output.toByteArray();
    }

    private static EncodedWord toEncodedWord(Matcher matcher) {
        return new EncodedWord(
                matcher.group(1),
                matcher.group(2).toUpperCase(Locale.ROOT).charAt(0),
                matcher.group(3),
                matcher.group(0)
        );
    }

    private static Charset toCharset(String charset) {
        try {
            return Charset.forName(MimeUtility.javaCharset(charset));
        } catch (Exception ignored) {
            try {
                return Charset.forName(charset);
            } catch (Exception ignoredAgain) {
                return StandardCharsets.UTF_8;
            }
        }
    }

    private static boolean containsEncodedWord(String value) {
        return value != null && ENCODED_WORD_PATTERN.matcher(value).find();
    }

    private static boolean containsReplacement(String value) {
        return value != null && value.indexOf('\uFFFD') >= 0;
    }

    private static boolean isHealthyDecodedText(String value) {
        if (isBlank(value)) {
            return false;
        }
        if (containsReplacement(value) || containsEncodedWord(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isISOControl(current) && current != '\r' && current != '\n' && current != '\t') {
                return false;
            }
        }
        return true;
    }

    private static boolean isWhitespaceOnly(String value, int start, int end) {
        for (int i = start; i < end; i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String bestCandidate(String standard, String decoded) {
        if (isBlank(standard)) {
            return decoded;
        }
        if (isBlank(decoded)) {
            return standard;
        }
        int standardScore = qualityScore(standard);
        int decodedScore = qualityScore(decoded);
        return decodedScore >= standardScore ? decoded : standard;
    }

    private static int qualityScore(String value) {
        if (value == null) {
            return Integer.MIN_VALUE;
        }
        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\uFFFD') {
                score -= 100;
            } else if (Character.isISOControl(current) && current != '\r' && current != '\n' && current != '\t') {
                score -= 50;
            } else if (current >= 0x4E00 && current <= 0x9FFF) {
                score += 3;
            } else if (current < 128 && !Character.isISOControl(current)) {
                score += 1;
            } else {
                score += 2;
            }
        }
        Matcher matcher = ENCODED_WORD_PATTERN.matcher(value);
        while (matcher.find()) {
            score -= 200;
        }
        return score;
    }

    private static String normalizeHeaderValue(String value) {
        String normalized = stripHeaderName(value);
        normalized = normalized.replaceAll("\\r?\\n(?=\\s*=\\?)", "\r\n ");
        return normalized;
    }

    private static String stripHeaderName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, "Subject:", 0, "Subject:".length())) {
            return trimmed.substring("Subject:".length()).trim();
        }
        return value;
    }

    private static String unfold(String value) {
        return value.replaceAll("\\r?\\n[\\t ]+", " ");
    }

    private static String cleanBase64(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class EncodedWord {
        private final String charset;
        private final char encoding;
        private final String encodedText;
        private final String original;

        private EncodedWord(String charset, char encoding, String encodedText, String original) {
            this.charset = charset;
            this.encoding = encoding;
            this.encodedText = encodedText;
            this.original = original;
        }

        private boolean isBase64() {
            return encoding == 'B';
        }

        private boolean sameEncoding(EncodedWord other) {
            return other != null
                    && encoding == other.encoding
                    && normalizedCharset().equals(other.normalizedCharset());
        }

        private String normalizedCharset() {
            return charset == null ? "" : charset.trim().toLowerCase(Locale.ROOT);
        }
    }
}
