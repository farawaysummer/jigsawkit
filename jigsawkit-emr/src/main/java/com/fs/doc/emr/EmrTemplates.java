package com.fs.doc.emr;

import com.fs.doc.emr.extractor.Extractor;
import com.fs.doc.emr.extractor.ValueType;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.commons.text.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class EmrTemplates {
    private static final String BASE_TEMPLATE = "base.xml";

    public static final EmrTemplates INSTANCE = new EmrTemplates(null);

    private final String templatePath;

    private EmrTemplates(String templatePath) {
        if (templatePath != null && Files.isDirectory(Paths.get(templatePath))) {
            this.templatePath = Paths.get(templatePath).toAbsolutePath() + "/";
        } else {
            this.templatePath = templatePath;
        }
    }

    public static EmrTemplates templates(String basePath) {
        return new EmrTemplates(basePath);
    }

    /**
     * 获取已有文档模板
     *
     * @param type
     * @return
     */
    public EmrTemplate newTemplate(String type) {
        TemplateBuilder builder = new TemplateBuilder(type);

        return builder.build();
    }

    public EmrTemplate newTemplate(String type, InputStream... projectResource) throws Exception {
        TemplateBuilder builder = new TemplateBuilder(type);

        for (InputStream inputStream : projectResource) {
            builder.merge(inputStream);
        }

        return builder.build();
    }

    public EmrTemplate[] newEvalTemplates(String type, InputStream... projectResource) throws Exception {
        EmrTemplate[] templates;
        if (projectResource.length == 0) {
            templates = new EmrTemplate[1];
            templates[0] = newTemplate(type);
            return templates;
        }

        templates = new EmrTemplate[projectResource.length];
        for (int index = 0; index < projectResource.length; index++) {
            InputStream inputStream = projectResource[index];
            TemplateBuilder builder = new TemplateBuilder(type);
            builder.merge(inputStream);
            templates[index] = builder.build();
        }

        return templates;
    }

    private class TemplateBuilder {
        private String name;
        private final Map<String, EmrLabel> labelMap;
        private final Map<String, EmrLabel> rootLabelMap;
        private final Map<String, Extractor> extractorMap;

        TemplateBuilder() {
            labelMap = Maps.newHashMap();
            rootLabelMap = Maps.newHashMap();
            extractorMap = Maps.newHashMap();
        }

        public TemplateBuilder(String type) {
            this();

            InputStream baseInputStream = null;
            try {
                baseInputStream = getTemplateStream("base");
            } catch (FileNotFoundException e) {
                //ignore
            }

            if (baseInputStream != null) {
                try {
                    parse(baseInputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                InputStream inputStream = getTemplateStream(type);
                if (inputStream != null) {
                    parse(inputStream);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private InputStream getTemplateStream(String type) throws FileNotFoundException {
            if (Strings.isNullOrEmpty(templatePath)) {
                return this.getClass().getClassLoader().getResourceAsStream("templates/" + type + ".xml");
            } else {
                return new FileInputStream(templatePath + type + ".xml");
            }
        }

        public TemplateBuilder name(String name) {
            this.name = name;

            return this;
        }

        public TemplateBuilder parse(InputStream inputStream) throws Exception {
            doParse(inputStream);

            return this;
        }

        public TemplateBuilder merge(InputStream inputStream) throws Exception {
            doParse(inputStream);

            return this;
        }

        private void doParse(InputStream inputStream) throws DocumentException {
            SAXReader reader = new SAXReader();
            Document document;
            document = reader.read(inputStream);
            this.name = document.getRootElement().attributeValue("name");

            Element extractorsElement = document.getRootElement().element("extractors");
            if (extractorsElement != null) {
                parseExtractor(extractorsElement);
            }

            Element labelsElement = document.getRootElement().element("labels");
            if (labelsElement != null) {
                List<Element> labelElements = labelsElement.elements("label");
                for (Element labelElement : labelElements) {
                    parseLabel(labelElement);
                }
            }

            Element constructElement = document.getRootElement().element("construct");
            if (constructElement != null) {
                parseConstruct(constructElement);
            }
        }

        public EmrTemplate build() {
            return new EmrTemplate(name, labelMap, rootLabelMap, extractorMap);
        }

        private void parseExtractor(Element extractor) {
            List<Element> extractors = extractor.elements("extractor");
            for (Element element : extractors) {
                ValueType type = ValueType.valueOf(element.attributeValue("type"));

                switch (type) {
                    case regex_group:
                        parseRegexGroupExtractor(element);
                        break;
                    default:
                        break;
                }
            }
        }

        private void parseRegexGroupExtractor(Element element) {
            String name = element.attributeValue("name");
            Element regexElement = element.element("regex");
            if (regexElement == null) {
                return;
            }

            String regex = regexElement.getText();
            Element groups = element.element("groups");
            if (groups == null) {
                return;
            }

            Map<String, String> groupMap = Maps.newHashMap();
            List<Element> groupElements = groups.elements("group");
            for (Element groupElement : groupElements) {
                String groupName = groupElement.attributeValue("name");
                String labelName = groupElement.attributeValue("label");
                if (Strings.isNullOrEmpty(groupName) || Strings.isNullOrEmpty(labelName)) {
                    continue;
                }

                groupMap.put(groupName, labelName);
            }

            extractorMap.put(name, Extractor.regexGroup(regex, groupMap));
        }

        private void parseLabel(Element labelNode) {
            String labelName = labelNode.attributeValue("name");
            EmrLabel label = labelMap.get(labelName);
            if (label == null) {
                String type = labelNode.attributeValue("type");
                String extractorName = labelNode.attributeValue("extractor");
                String desc = labelNode.attributeValue("desc");
                label = new EmrLabel(labelName);
                label.setDesc(desc);

                label.setExtractor(extractorByType(type, extractorName));
            } else {
                String type = labelNode.attributeValue("type");
                String extractorName = labelNode.attributeValue("extractor");

                label.setExtractor(extractorByType(type, extractorName));
            }

            Element titleElements = labelNode.element("titles");
            if (titleElements != null) {
                List<Element> titles = titleElements.elements("title");
                for (Element title : titles) {
                    String disable = labelNode.attributeValue("disable");
                    String titleContent = StringEscapeUtils.unescapeXml(title.getText());
                    if (Strings.isNullOrEmpty(titleContent)) {
                        continue;
                    }
                    if (Boolean.parseBoolean(disable)) {
                        label.removeTitle(titleContent);
                    } else {
                        label.addTitle(titleContent);
                    }
                }
            }

            labelMap.put(labelName, label);
        }

        private void parseConstruct(Element constructNode) {
            List<Element> labels = constructNode.elements("label");
            for (Element label : labels) {
                String labelName = label.attributeValue("name");
                EmrLabel existLabel = labelMap.get(labelName);

                if (existLabel == null) {
                    continue;
                }

                if (!label.elements().isEmpty()) {
                    if (isSimpleType(existLabel.getType())) {
                        existLabel.setExtractor(Extractor.complex());
                    }
                    parseLabelConstruct(label, existLabel);
                }

                rootLabelMap.put(labelName, existLabel);
            }
        }

        private void parseLabelConstruct(Element labelNode, EmrLabel parentLabel) {
            List<Element> labels = labelNode.elements("label");
            for (Element label : labels) {
                String labelName = label.attributeValue("name");
                EmrLabel existLabel = labelMap.get(labelName);

                if (existLabel == null) {
                    continue;
                }

                if (!label.elements().isEmpty()) {
                    if (isSimpleType(existLabel.getType())) {
                        existLabel.setExtractor(Extractor.complex());
                    }
                    parseLabelConstruct(label, existLabel);
                }

                parentLabel.addSubLabel(existLabel);
            }
        }

        private boolean isSimpleType(ValueType type) {
            return type == ValueType.string
                    || type == ValueType.datetime
                    || type == ValueType.number
                    || type == ValueType.exclude;
        }

        private Extractor extractorByType(String typeStr, String extName) {
            ValueType type = ValueType.valueOf(typeStr);
            switch (type) {
                case string:
                    return Extractor.string();
                case number:
                    return Extractor.number();
                case datetime:
                    return Extractor.datetime();
                case complex:
                    return Extractor.complex();
                case regex_group:
                    return extractorMap.get(extName);
                case combine:
                    return Extractor.combine(extName);
                default:
                    return Extractor.exclude();
            }
        }
    }
}
