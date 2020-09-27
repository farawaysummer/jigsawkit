package com.fs.doc.emr.extractor;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractorUtils {
    private static final Map<String, String> DATE_FORMAT_REGEXPS = Maps.newLinkedHashMap();

    private static final Map<String, Pattern> PATTERN_MAP = Maps.newHashMap();

    static {
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}年\\d{1,2}月\\d{1,2}日\\s{1,2}时\\d{2}分\\d{2}秒).*", "yyyy年MM月dd日 HH时mm分ss秒");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}年\\d{1,2}月\\d{1,2}日{1,2}时\\d{2}分\\d{2}秒).*", "yyyy年MM月dd日HH时mm分ss秒");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}).*", "yyyy/MM/dd HH:mm:ss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}).*", "dd-MM-yyyy HH:mm:ss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}).*", "yyyy-MM-dd HH:mm:ss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}).*", "MM/dd/yyyy HH:mm:ss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}).*", "dd MMM yyyy HH:mm:ss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}).*", "dd MMMM yyyy HH:mm:ss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}).*", "dd MMM yyyy HH:mm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}).*", "dd MMMM yyyy HH:mm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}).*", "dd-MM-yyyy HH:mm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}).*", "yyyy-MM-dd HH:mm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}).*", "MM/dd/yyyy HH:mm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}).*", "yyyy/MM/dd HH:mm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}年\\d{1,2}月\\d{1,2}日\\d{1,2}时\\d{2}分).*", "yyyy年MM月dd日HH时mm分");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}-\\d{1,2}-\\d{1,2}\\d{1,2}:\\d{2}).*", "yyyy-MM-ddHH:mm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{8}\\s\\d{6}).*", "yyyyMMdd HHmmss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{14}).*", "yyyyMMddHHmmss");
        DATE_FORMAT_REGEXPS.put(".*(\\d{8}\\s\\d{4}).*", "yyyyMMdd HHmm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{12}).*", "yyyyMMddHHmm");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}\\s[a-z]{3}\\s\\d{4}).*", "dd MMM yyyy");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}\\s[a-z]{4,}\\s\\d{4}).*", "dd MMMM yyyy");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}年\\d{1,2}月\\d{1,2}日).*", "yyyy年MM月dd日");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}-\\d{1,2}-\\d{4}).*", "dd-MM-yyyy");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}-\\d{1,2}-\\d{1,2}).*", "yyyy-MM-dd");
        DATE_FORMAT_REGEXPS.put(".*(\\d{1,2}/\\d{1,2}/\\d{4}).*", "MM/dd/yyyy");
        DATE_FORMAT_REGEXPS.put(".*(\\d{4}/\\d{1,2}/\\d{1,2}).*", "yyyy/MM/dd");
        DATE_FORMAT_REGEXPS.put(".*(\\d{8}).*", "yyyyMMdd");

        for (Map.Entry<String, String> entry : DATE_FORMAT_REGEXPS.entrySet()) {
            PATTERN_MAP.put(entry.getValue(), Pattern.compile(entry.getKey()));
        }
    }

    public static String normalizeDocument(String input) {
        String dbcDoc = toDBC(input);
        String cleanDoc = removeDuplicateBlank(dbcDoc);
        return cleanDoc.trim();
    }

    public static String toDBC(String input) {
        char c[] = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '\u3000') {
                c[i] = ' ';
            } else if (c[i] > '\uFF00' && c[i] < '\uFF5F') {
                c[i] = (char) (c[i] - 65248);
            }
        }
        return new String(c);
    }

    public static String removeDuplicateBlank(String input) {
        StringBuilder processed = new StringBuilder();

        int index = 0;
        char last = 0;
        while (index < input.length()) {
            char c = input.charAt(index);
            if (CharMatcher.breakingWhitespace().matches(c) && last == c) {
                index++;
                continue;
            }

            processed.append(c);
            last = c;
            index++;
        }

        return processed.toString();
    }

    public static String extractNumber(String string) {
        StringBuilder number = new StringBuilder();
        boolean hasFound = false;
        boolean hasDot = false;
        for (int index = 0; index < string.length(); index++) {
            char c = string.charAt(index);
            if (c == '-' && !hasFound && index < string.length() - 1 && isNumberChar(string.charAt(index + 1))) {
                number.append(c);
                hasFound = true;
                continue;
            }

            if (c == '.'
                    && hasFound
                    && !hasDot
                    && index < string.length() - 1
                    && isNumberChar(string.charAt(index - 1))
                    && isNumberChar(string.charAt(index + 1))) {
                number.append(c);
                hasDot = true;
                continue;
            }

            if (!isNumberChar(c)) {
                if (!hasFound) {
                    continue;
                }

                return number.toString();
            }

            number.append(c);
            hasFound = true;
        }

        return number.toString();
    }

    public static boolean isNumberChar(char c) {
        return c >= '0' && c <= '9';
    }

    public static String extractDate(String dateString) {
        String cleanDateString = dateString.replace("\r\n", " ").replace('\n',' ');
        String dateFormat = determineDateFormat(cleanDateString);
        if (dateFormat == null) {
            //do log error
            return dateString;
        }

        Pattern pattern = PATTERN_MAP.get(dateFormat);
        Matcher matcher = pattern.matcher(cleanDateString);

        if (matcher.matches() && matcher.groupCount() >= 1) {
            return matcher.group(1);
        }

        return dateString;
    }

    public static String determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return DATE_FORMAT_REGEXPS.get(regexp);
            }
        }

        return null; // Unknown format.
    }

    public static String extractChinese(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder string = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (isChineseByScript(c)) {
                string.append(c);
            }
        }

        return string.toString();
    }

    public static String extractContent(String input) {
        //run script to extract content from input string
        return input;
    }

    public static boolean isEnPunc(char ch){
        if (0x21 <= ch && ch <= 0x22) return true;
        if (ch == 0x27 || ch == 0x2C) return true;
        if (ch == 0x2E || ch == 0x3A) return true;
        if (ch == 0x3B || ch == 0x3F) return true;

        return false;
    }

    //使用UnicodeBlock方法判断
    public static boolean isChineseByBlock(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isChineseByScript(char c) {
        Character.UnicodeScript sc = Character.UnicodeScript.of(c);
        if (sc == Character.UnicodeScript.HAN) {
            return true;
        }

        return false;
    }

    // 根据UnicodeBlock方法判断中文标点符号
    public static boolean isChinesePunctuation(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
                || ub == Character.UnicodeBlock.VERTICAL_FORMS) {
            return true;
        } else {
            return false;
        }
    }

    public static String trimPunctuation(String str) {

        return PunMatcher.INSTANCE.trimFrom(str);
    }

    public static String trimTailPunctuation(String str) {
        return PunMatcher.INSTANCE.trimTrailingFrom(str);
    }

    private static class PunMatcher extends CharMatcher {
        static final PunMatcher INSTANCE = new PunMatcher();

        @Override
        public boolean matches(char c) {

            return isChinesePunctuation(c) || CharMatcher.whitespace().matches(c) || isEnPunc(c);
        }
    }

}
