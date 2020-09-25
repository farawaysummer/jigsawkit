package com.fs.doc.jigsaw.trainer;

import com.fs.doc.jigsaw.Label;
import com.fs.doc.jigsaw.Template;
import com.fs.doc.jigsaw.Templates;
import com.fs.doc.jigsaw.extractor.ValueType;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.FileInputStream;
import java.util.Map;
import java.util.Objects;

public class TemplateTrainer {
    private final DocumentAnalyzer analyzer;
    private final LoadingCache<CacheTemplateIndex, Map<String, Label>> labelMapCache;
    private final Templates templates;

    public TemplateTrainer(final DocumentAnalyzer analyzer, final String templatePath) {
        this.analyzer = analyzer;
        if (templatePath == null) {
            templates = Templates.INSTANCE;
        } else {
            this.templates = Templates.templates(templatePath);
        }

        labelMapCache = CacheBuilder.newBuilder().build(
                new CacheLoader<CacheTemplateIndex, Map<String, Label>>() {
                    @Override
                    public Map<String, Label> load(CacheTemplateIndex cacheIndex) throws Exception {
                        Template template;
                        if (Strings.isNullOrEmpty(cacheIndex.projectSource)) {
                            template = templates.newTemplate(cacheIndex.templateType);
                        } else {
                            template = templates.newTemplate(cacheIndex.templateType,
                                    new FileInputStream(cacheIndex.projectSource));
                        }

                        return template.getNormalizedLabelMap(analyzer.getSeparatorMatcher());
                    }
                }
        );
    }

    public TemplateTrainer(final DocumentAnalyzer analyzer) {
        this.analyzer = analyzer;
        templates = Templates.INSTANCE;

        labelMapCache = CacheBuilder.newBuilder().build(
                new CacheLoader<CacheTemplateIndex, Map<String, Label>>() {
                    @Override
                    public Map<String, Label> load(CacheTemplateIndex cacheIndex) throws Exception {
                        Template template;
                        if (Strings.isNullOrEmpty(cacheIndex.projectSource)) {
                            template = templates.newTemplate(cacheIndex.templateType);
                        } else {
                            template = templates.newTemplate(cacheIndex.templateType,
                                    new FileInputStream(cacheIndex.projectSource));
                        }

                        return template.getNormalizedLabelMap(analyzer.getSeparatorMatcher());
                    }
                }
        );
    }

    public Template train(String doc, String docType, String... projectSources) throws Exception {
        String selectedSource = null;
        Map<String, Label> fitLabelMaps = null;
        for (String projectSource : projectSources) {
            Map<String, Label> labelMap = labelMapCache.get(new CacheTemplateIndex(docType, projectSource));
            Map<String, Label> fitLabels = analyzer.analyze(doc, labelMap);

            if (fitLabelMaps == null || fitLabelMaps.size() < fitLabels.size()) {
                fitLabelMaps = fitLabels;
                selectedSource = projectSource;
            }
        }

        if (selectedSource == null || fitLabelMaps == null) {
            Template template = templates.newTemplate(docType);
            return template.copyTemplate();
        }

        System.out.println(fitLabelMaps);

        Template template = templates.newTemplate(docType, new FileInputStream(selectedSource));
        Template toTrained = template.copyTemplate();

        doTrain(fitLabelMaps, toTrained);

        return toTrained;
    }

    public Template train(String doc, String docType, String projectSource, Template lastTrained) throws Exception {
        Map<String, Label> labelMap = labelMapCache.get(new CacheTemplateIndex(docType, projectSource));
        Map<String, Label> fitLabels = analyzer.analyze(doc, labelMap);
        System.out.println(fitLabels);
        Template toTrained;
        if (lastTrained == null) {
            Template template = templates.newTemplate(docType, new FileInputStream(projectSource));
            toTrained = template.copyTemplate();
        } else {
            toTrained = lastTrained;
        }

        doTrain(fitLabels, toTrained);

        return toTrained;
    }

    private void doTrain(Map<String, Label> fitLabels, Template toTrained) {
        for (Map.Entry<String, Label> entry : fitLabels.entrySet()) {
            Label label = entry.getValue();
            if (label == null) continue;

            Label templateLabel = toTrained.getLabelMap().get(label.getName());
            if (templateLabel == null) {
                System.err.println("Can't find label " + label.getName());
            } else {
                if (label.getType() == ValueType.exclude) {
                    templateLabel.addTitle(entry.getKey());
                } else {
                    templateLabel.replaceTitle(entry.getKey());
                }
            }
        }
    }

    static class CacheTemplateIndex {
        String templateType;
        String projectSource;

        public CacheTemplateIndex(String templateType, String projectSource) {
            this.templateType = templateType;
            this.projectSource = projectSource;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheTemplateIndex)) return false;
            CacheTemplateIndex that = (CacheTemplateIndex) o;
            return Objects.equals(templateType, that.templateType) &&
                    Objects.equals(projectSource, that.projectSource);
        }

        @Override
        public int hashCode() {
            return Objects.hash(templateType, projectSource);
        }
    }
}
