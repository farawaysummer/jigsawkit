package com.fs.doc.emr;

import com.fs.doc.emr.extractor.ValueType;
import com.google.common.collect.Maps;

import java.util.Map;

public class JigsawResult {
    private final Map<String, String> values;
    private final Map<String, JigsawEmrPart> parts;
    private int docLength = 1;
    private int parsedLength = 0;
    private int expectPartCount = 0;
    private int partCount = 0;

    public JigsawResult() {
        parts = Maps.newHashMap();
        values = Maps.newHashMap();
    }

    public void recordDocLength(int length) {
        this.docLength = length;
    }

    public void addPart(String labelName, String title, EmrLabel label) {
        JigsawEmrPart part = new JigsawEmrPart();
        part.setTitle(title);
        part.setLabel(label);

        if (label.getType() != ValueType.complex && label.getType() != ValueType.exclude) {
            expectPartCount++;
        }

        parts.put(labelName, part);
    }

    public void setPartValue(String labelName, String value) {
        JigsawEmrPart part = parts.get(labelName);
        if (part != null) {
            if (part.getLabel().getType() != ValueType.complex) {
                parsedLength += value.length();
                partCount++;

            }

            part.setValue(value);
        }

        values.put(labelName, value);
    }

    public Map<String, String> getResultValues() {

        return values;
    }

    public float calculateCoverRate() {
        return (float)parsedLength / (float)docLength;
    }

    public int getExpectedPartCount() {
        return expectPartCount;
    }

    public int getParsedPartCount() {
        return partCount;
    }
}
