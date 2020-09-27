package com.fs.doc.emr;

import com.fs.doc.emr.extractor.Extractor;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EmrTemplate {
    private final String templateName;
    private final Map<String, EmrLabel> labelMap;
    private final Map<String, EmrLabel> rootLabelMap;
    private final Map<String, Extractor> extractorMap;

    protected EmrTemplate(String templateName,
                          Map<String, EmrLabel> labelMap,
                          Map<String, EmrLabel> rootLabelMap,
                          Map<String, Extractor> extractorMap) {
        this.templateName = templateName;
        this.labelMap = labelMap;
        this.rootLabelMap = rootLabelMap;
        this.extractorMap = extractorMap;
    }

    public Map<String, EmrLabel> getRootLabelMap() {
        return rootLabelMap;
    }

    public Map<String, EmrLabel> getLabelMap() {
        return labelMap;
    }

    public Map<String, EmrLabel> getNormalizedLabelMap(CharMatcher separatorMatcher) {
        Collection<EmrLabel> labels = getLabelMap().values();
        Map<String, EmrLabel> normalizedMap = Maps.newHashMap();
        for (EmrLabel label : labels) {
            Set<String> labelTitles = Sets.newHashSet();

            for (String optionTitle : label.getOptionalTitles()) {
                labelTitles.add(normalizeTitle(optionTitle, separatorMatcher));
            }

            for (String labelTitle : labelTitles) {
                normalizedMap.put(labelTitle, label);
            }
        }

        return normalizedMap;
    }

    public EmrTemplate copyTemplate() {
        Map<String, EmrLabel> copyRootLabel = Maps.newHashMap();
        for (Map.Entry<String, EmrLabel> entry : rootLabelMap.entrySet()) {
            copyRootLabel.put(entry.getKey(), entry.getValue().clone());
        }

        return constructCloneTemplate(copyRootLabel);
    }

    private EmrTemplate constructCloneTemplate(Map<String, EmrLabel> copyRootLabel) {
        List<EmrLabel> nestLabels = Lists.newArrayList();
        for (EmrLabel label : copyRootLabel.values()) {
            label.getNestLabels(nestLabels);
            nestLabels.add(label);
        }

        Map<String, EmrLabel> copyLabelMap = Maps.newHashMap();
        for (EmrLabel label : nestLabels) {
            copyLabelMap.put(label.getName(), label);
        }

        return new EmrTemplate(this.templateName, copyLabelMap, copyRootLabel, extractorMap);
    }

    public String export() {

        return null;
    }

    public static String normalizeTitle(String field, CharMatcher matcher) {
        return matcher.or(CharMatcher.whitespace()).removeFrom(field);
    }

    @Override
    public String toString() {
        return "Template{" +
                "templateName='" + templateName + '\'' +
                ", labelMap=" + labelMap +
                ", rootLabelMap=" + rootLabelMap +
                '}';
    }
}
