package com.fs.doc.jigsaw.eval;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

class ResourceGenerator {

    public static void main(String[] args) throws IOException {
        ResourceGenerator generator = new ResourceGenerator();
        generator.generateAll("C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents",
                "INP_OUT_TRANS",
                "C:\\starless\\privates\\workspaces\\jigsaw\\INP_OUT_TRANS.txt");
    }

    public void generateAll(String docBasePath, String templateName, String defFile) throws IOException {
        // create directory
        File docBaseDir = new File(docBasePath);
        if (!docBaseDir.isDirectory()) {
            throw new IOException("Document base path must be directory.");
        }

        String templateBasePath = docBaseDir.getAbsolutePath() + "/" + templateName + "/";
        File templateBaseDir = new File(templateBasePath);
        if (!templateBaseDir.exists()) {
            templateBaseDir.mkdirs();

            new File(templateBasePath + "material/").mkdir();
            new File(templateBasePath + "output/").mkdir();
            new File(templateBasePath + "projects/").mkdir();
            new File(templateBasePath + "struct/").mkdir();
            new File(templateBasePath + "template/").mkdir();
        }

        // copy resource
        String targetDefPath = templateBasePath + "struct/" + templateName + ".txt";
        Files.move(new File(defFile), new File(targetDefPath));

        // generate template
        generateTemplate(templateBasePath + "template/", templateName, targetDefPath);

        // generate project file
        generateProjectFile(templateBasePath + "projects/", templateName.toLowerCase(), templateName, targetDefPath);
    }

    public void generateTemplate(String basePath, String templateName, String defFile) throws IOException {
        Document document = DocumentHelper.createDocument();
        document.setXMLEncoding("UTF-8");

        Element rootElement = document.addElement("template");
        rootElement.addAttribute("name", templateName);

        //add labels
        final Element labels = rootElement.addElement("labels");
        Set<String> lines = Sets.newHashSet(Files.readLines(new File(defFile), Charset.defaultCharset()));
        for (String line : lines) {
            if (Strings.isNullOrEmpty(line)) {
                continue;
            }

            List<String> parts = Splitter.on(CharMatcher.breakingWhitespace()).splitToList(line);
            String fieldName = parts.get(1);
            if (fieldName.endsWith("CODE")) {
                continue;
            }

            Element label = labels.addElement("label");
            label.addAttribute("name", fieldName);
            System.out.println(fieldName);
            label.addAttribute("desc", parts.get(0));

            String type = "string";
            if (fieldName.endsWith("DTIME") || fieldName.endsWith("DATE")) {
                type = "datetime";
            }

            label.addAttribute("type", type);

            Element titles = label.addElement("titles");
            Element title = titles.addElement("title");
            title.setText(" ");
        }

        //add constructs
        rootElement.addElement("construct");

        OutputFormat format = OutputFormat.createPrettyPrint();  //转换成字符串
        format.setEncoding("UTF-8");
        FileOutputStream fos = new FileOutputStream(basePath + templateName + ".xml");
        XMLWriter writer = new XMLWriter(fos, format);
        writer.write(document);
    }

    public void generateProjectFile(String basePath, String projectName, String templateName, String defFile)
            throws IOException {
        Document document = DocumentHelper.createDocument();
        document.setXMLEncoding("UTF-8");

        Element rootElement = document.addElement("project");
        rootElement.addAttribute("name", projectName);
        rootElement.addAttribute("type", templateName);

        //add labels
        final Element labels = rootElement.addElement("labels");

        //add base info label
        Element baseInfoElement = labels.addElement("label");
        baseInfoElement.addAttribute("name", "BASE_INFO");
        baseInfoElement.addAttribute("desc", "BASE_INFO");
        baseInfoElement.addAttribute("type", "complex");
        Element titlesElement = baseInfoElement.addElement("titles");
        titlesElement.addElement("title").setText("*");

        //add ext label
        Element extElement = labels.addElement("label");
        extElement.addAttribute("name", "EXT");
        extElement.addAttribute("desc", "EXT");
        extElement.addAttribute("type", "exclude");
        extElement.addElement("titles").addElement("title").setText("");

        Set<String> lines = Sets.newHashSet(Files.readLines(new File(defFile), Charset.defaultCharset()));
        for (String line : lines) {
            if (Strings.isNullOrEmpty(line)) {
                continue;
            }

            List<String> parts = Splitter.on(CharMatcher.breakingWhitespace()).splitToList(line);
            String fieldDesc = parts.get(0);
            String fieldName = parts.get(1);
            if (fieldName.endsWith("CODE")) {
                continue;
            }

            Element label = labels.addElement("label");
            label.addAttribute("name", fieldName);
            System.out.println(fieldName);
            label.addAttribute("desc", fieldDesc);

            String type = "string";
            if (fieldName.endsWith("DTIME") || fieldName.endsWith("DATE")
                    || fieldDesc.endsWith("日期") || fieldDesc.endsWith("时间")) {
                type = "datetime";
            }

            label.addAttribute("type", type);

            Element titles = label.addElement("titles");
            Element title = titles.addElement("title");
            title.setText(" ");
        }

        Element defaultConstruct = rootElement.addElement("construct");
        for (String line : lines) {
            if (Strings.isNullOrEmpty(line)) {
                continue;
            }

            List<String> parts = Splitter.on(CharMatcher.breakingWhitespace()).splitToList(line);
            String fieldName = parts.get(1);
            if (fieldName.endsWith("CODE")) {
                continue;
            }

            defaultConstruct.addComment(String.format("<label name=\"%s\" desc=\"%s\"/>", fieldName, parts.get(0)));
        }

        OutputFormat format = OutputFormat.createPrettyPrint();  //转换成字符串
        format.setEncoding("UTF-8");
        FileOutputStream fos = new FileOutputStream(String.format("%sproject_%s.xml", basePath, projectName));
        XMLWriter writer = new XMLWriter(fos, format);
        writer.write(document);
    }

}
