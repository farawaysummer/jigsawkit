package com.fs.doc.emr.extractor;

import com.fs.doc.emr.EmrLabel;
import com.fs.doc.emr.JigsawResult;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Extractor {
    private final ValueType valueType;


    public abstract void extractValue(EmrLabel label, String value, JigsawResult result);

    protected Extractor(ValueType type) {
        this.valueType = type;
    }

    public ValueType type() {
        return valueType;
    }

    public static Extractor string() {
        return StringExtractor.INSTANCE;
    }

    public static Extractor number() {
        return NumberExtractor.INSTANCE;
    }

    public static Extractor datetime() {
        return DatetimeExtractor.INSTANCE;
    }

    public static Extractor complex() {
        return ComplexExtractor.INSTANCE;
    }

    public static Extractor exclude() {
        return DullExtractor.INSTANCE;
    }

    public static Extractor regexGroup(String regex, Map<String, String> groups) {
        return new RegexGroupExtractor(regex, groups);
    }

    public static Extractor combine(String combineStr) {
        return new CombineExtractor(combineStr);
    }

    /**
     * extract string value as original format
     */
    private static final class StringExtractor extends Extractor {
        static final StringExtractor INSTANCE = new StringExtractor();

        private StringExtractor() {
            super(ValueType.string);
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {
            String extractValue = ExtractorUtils.trimPunctuation(value);
            result.setPartValue(label.getName(), extractValue);
        }
    }

    /**
     * extract number value from string
     */
    private static final class NumberExtractor extends Extractor {
        static final NumberExtractor INSTANCE = new NumberExtractor();

        private NumberExtractor() {
            super(ValueType.number);
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {
            String extractValue = ExtractorUtils.extractNumber(value);
            result.setPartValue(label.getName(), extractValue);
        }
    }

    /**
     * extract datetime value from string
     */
    private static final class DatetimeExtractor extends Extractor {
        static final DatetimeExtractor INSTANCE = new DatetimeExtractor();

        private DatetimeExtractor() {
            super(ValueType.datetime);
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {
            String extractValue = ExtractorUtils.extractDate(value);

            result.setPartValue(label.getName(), extractValue);
        }
    }

    private static final class ComplexExtractor extends Extractor {
        static final ComplexExtractor INSTANCE = new ComplexExtractor();

        private ComplexExtractor() {
            super(ValueType.complex);
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {
            result.setPartValue(label.getName(), value);

        }
    }

    private static final class CombineExtractor extends Extractor {
        private final String combine;
        private final List<String> fields;

        private CombineExtractor(String combine) {
            super(ValueType.combine);
            this.combine = combine;
            fields = Lists.newArrayList();

            int position = combine.indexOf("${");
            while (position != -1) {
                int endPosition = combine.indexOf("}", position);
                if (endPosition == -1) {
                    break;
                }

                fields.add(combine.substring(position + 2, endPosition));
                position = combine.indexOf("${", endPosition);
            }
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {
            String resultValue = combine;
            boolean hasValue = false;
            for (String field : fields) {
                String fieldValue = result.getResultValues().get(field);
                if (!Strings.isNullOrEmpty(fieldValue)) {
                    hasValue = true;
                }
            }

            for (String field : fields) {
                String fieldValue = result.getResultValues().get(field);
                resultValue = resultValue.replace("${" + field + "}", Strings.nullToEmpty(fieldValue));
            }

            if (hasValue) {
                result.setPartValue(label.getName(), resultValue);
            }
        }
    }

    private static final class RegexGroupExtractor extends Extractor {
        private final Pattern pattern;
        private final Map<String, String> groupMaps;

        private RegexGroupExtractor(String regex, Map<String, String> groupMaps) {
            super(ValueType.regex_group);
            this.pattern = Pattern.compile(regex);
            this.groupMaps = Maps.newHashMap(groupMaps);
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {
            result.setPartValue(label.getName(), value);

            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                for (Map.Entry<String, String> entry : groupMaps.entrySet()) {
                    try {
                        String groupValue = matcher.group(entry.getKey());
                        if (!Strings.isNullOrEmpty(groupValue)) {
                            result.setPartValue(entry.getValue(), groupValue);
                        }
                    } catch (IllegalArgumentException e) {
                        //ignore
                    }
                }
            }
        }
    }

    private static final class ScriptExtractor extends Extractor {

        private ScriptExtractor() {
            super(ValueType.script);
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {

        }
    }

    private static final class DullExtractor extends Extractor {
        static final DullExtractor INSTANCE = new DullExtractor();

        private DullExtractor() {
            super(ValueType.exclude);
        }

        @Override
        public void extractValue(EmrLabel label, String value, JigsawResult result) {

        }
    }

}
