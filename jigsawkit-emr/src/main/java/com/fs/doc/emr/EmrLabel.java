package com.fs.doc.emr;

import com.fs.doc.emr.extractor.Extractor;
import com.fs.doc.emr.extractor.ValueType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EmrLabel {
    protected String name;
    protected String desc;
    protected Extractor extractor;

    private final List<String> optionalTitles;
    private final Map<String, EmrLabel> subLabels;

    public EmrLabel(String name) {
        this.name = name;
        optionalTitles = Lists.newArrayList();
        subLabels = Maps.newHashMap();
    }

    public void setExtractor(Extractor extractor) {
        this.extractor = extractor;
    }

    public Extractor getExtractor() {
        return extractor;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public ValueType getType() {
        if (extractor == null) {
            return ValueType.exclude;
        }

        return extractor.type();
    }

    public void removeTitle(String title) {
        this.optionalTitles.remove(title);
        sort();
    }

    public void replaceTitle(String title) {
        this.optionalTitles.clear();
        this.optionalTitles.add(title);
    }

    public void addTitle(String title) {
        if (optionalTitles.contains(title)) {
            return;
        }

        this.optionalTitles.add(title);
        sort();
    }

    private void sort() {
        Collections.sort(optionalTitles, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1.length() > o2.length()) {
                    return -1;
                } else if (o1.length() == o2.length()) {
                    return o1.compareTo(o2);
                } else {
                    return 1;
                }
            }
        });
    }

    public String getName() {
        return name;
    }

    public List<String> getOptionalTitles() {
        return optionalTitles;
    }

    public void addSubLabel(EmrLabel subLabel) {
        if (subLabel == null) {
            return;
        }

        this.subLabels.put(subLabel.getName(), subLabel);
    }

    public Map<String, EmrLabel> getSubLabels() {
        return subLabels;
    }

    public void getNestLabels(List<EmrLabel> labels) {
        labels.addAll(subLabels.values());
        for (EmrLabel label : subLabels.values()) {
            label.getNestLabels(labels);
        }
    }

    public EmrLabel clone() {
        EmrLabel label = new EmrLabel(this.name);
        label.desc = this.desc;
        label.extractor = this.extractor;

        label.optionalTitles.addAll(this.optionalTitles);

        for (Map.Entry<String, EmrLabel> entry : subLabels.entrySet()) {
            label.subLabels.put(entry.getKey(), entry.getValue().clone());
        }

        return label;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }
}
