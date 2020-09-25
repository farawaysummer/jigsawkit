package com.fs.doc.jigsaw;

import com.fs.doc.jigsaw.extractor.Extractor;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Template {
    private final String templateName;
    private final Map<String, Label> labelMap;
    private final Map<String, Label> rootLabelMap;
    private final Map<String, Extractor> extractorMap;

    protected Template(String templateName,
                       Map<String, Label> labelMap,
                       Map<String, Label> rootLabelMap,
                       Map<String, Extractor> extractorMap) {
        this.templateName = templateName;
        this.labelMap = labelMap;
        this.rootLabelMap = rootLabelMap;
        this.extractorMap = extractorMap;
    }

    public Map<String, Label> getRootLabelMap() {
        return rootLabelMap;
    }

    public Map<String, Label> getLabelMap() {
        return labelMap;
    }

    public Map<String, Label> getNormalizedLabelMap(CharMatcher separatorMatcher) {
        Collection<Label> labels = getLabelMap().values();
        Map<String, Label> normalizedMap = Maps.newHashMap();
        for (Label label : labels) {
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

    public Template copyTemplate() {
        Map<String, Label> copyRootLabel = Maps.newHashMap();
        for (Map.Entry<String, Label> entry : rootLabelMap.entrySet()) {
            copyRootLabel.put(entry.getKey(), entry.getValue().clone());
        }

        return constructCloneTemplate(copyRootLabel);
    }

    private Template constructCloneTemplate(Map<String, Label> copyRootLabel) {
        List<Label> nestLabels = Lists.newArrayList();
        for (Label label : copyRootLabel.values()) {
            label.getNestLabels(nestLabels);
            nestLabels.add(label);
        }

        Map<String, Label> copyLabelMap = Maps.newHashMap();
        for (Label label : nestLabels) {
            copyLabelMap.put(label.getName(), label);
        }

        return new Template(this.templateName, copyLabelMap, copyRootLabel, extractorMap);
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
