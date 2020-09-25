package com.fs.doc.jigsaw.trainer;

import com.fs.doc.jigsaw.EmrLabel;
import com.fs.doc.jigsaw.EmrTemplate;
import com.fs.doc.jigsaw.EmrTemplates;
import com.fs.doc.jigsaw.extractor.ValueType;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.FileInputStream;
import java.util.Map;
import java.util.Objects;

public class EmrTemplateTrainer {
    private final EmrDocumentAnalyzer analyzer;
    private final LoadingCache<CacheTemplateIndex, Map<String, EmrLabel>> labelMapCache;
    private final EmrTemplates templates;

    public EmrTemplateTrainer(final EmrDocumentAnalyzer analyzer, final String templatePath) {
        this.analyzer = analyzer;
        if (templatePath == null) {
            templates = EmrTemplates.INSTANCE;
        } else {
            this.templates = EmrTemplates.templates(templatePath);
        }

        labelMapCache = CacheBuilder.newBuilder().build(
                new CacheLoader<CacheTemplateIndex, Map<String, EmrLabel>>() {
                    @Override
                    public Map<String, EmrLabel> load(CacheTemplateIndex cacheIndex) throws Exception {
                        EmrTemplate template;
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

    public EmrTemplateTrainer(final EmrDocumentAnalyzer analyzer) {
        this.analyzer = analyzer;
        templates = EmrTemplates.INSTANCE;

        labelMapCache = CacheBuilder.newBuilder().build(
                new CacheLoader<CacheTemplateIndex, Map<String, EmrLabel>>() {
                    @Override
                    public Map<String, EmrLabel> load(CacheTemplateIndex cacheIndex) throws Exception {
                        EmrTemplate template;
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

    public EmrTemplate train(String doc, String docType, String... projectSources) throws Exception {
        String selectedSource = null;
        Map<String, EmrLabel> fitLabelMaps = null;
        for (String projectSource : projectSources) {
            Map<String, EmrLabel> labelMap = labelMapCache.get(new CacheTemplateIndex(docType, projectSource));
            Map<String, EmrLabel> fitLabels = analyzer.analyze(doc, labelMap);

            if (fitLabelMaps == null || fitLabelMaps.size() < fitLabels.size()) {
                fitLabelMaps = fitLabels;
                selectedSource = projectSource;
            }
        }

        if (selectedSource == null || fitLabelMaps == null) {
            EmrTemplate template = templates.newTemplate(docType);
            return template.copyTemplate();
        }

        System.out.println(fitLabelMaps);

        EmrTemplate template = templates.newTemplate(docType, new FileInputStream(selectedSource));
        EmrTemplate toTrained = template.copyTemplate();

        doTrain(fitLabelMaps, toTrained);

        return toTrained;
    }

    public EmrTemplate train(String doc, String docType, String projectSource, EmrTemplate lastTrained) throws Exception {
        Map<String, EmrLabel> labelMap = labelMapCache.get(new CacheTemplateIndex(docType, projectSource));
        Map<String, EmrLabel> fitLabels = analyzer.analyze(doc, labelMap);
        System.out.println(fitLabels);
        EmrTemplate toTrained;
        if (lastTrained == null) {
            EmrTemplate template = templates.newTemplate(docType, new FileInputStream(projectSource));
            toTrained = template.copyTemplate();
        } else {
            toTrained = lastTrained;
        }

        doTrain(fitLabels, toTrained);

        return toTrained;
    }

    private void doTrain(Map<String, EmrLabel> fitLabels, EmrTemplate toTrained) {
        for (Map.Entry<String, EmrLabel> entry : fitLabels.entrySet()) {
            EmrLabel label = entry.getValue();
            if (label == null) continue;

            EmrLabel templateLabel = toTrained.getLabelMap().get(label.getName());
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
