package com.fs.doc.jigsaw.eval;

import com.fs.doc.jigsaw.Jigsaw;
import com.fs.doc.jigsaw.JigsawResult;
import com.fs.doc.jigsaw.Label;
import com.fs.doc.jigsaw.Template;
import com.fs.doc.jigsaw.extractor.ValueType;
import com.fs.doc.jigsaw.trainer.DocumentAnalyzer;
import com.fs.doc.jigsaw.trainer.TemplateTrainer;
import com.google.common.io.Files;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

public class SingleDocEval {
    private final String templateName;
    private String[] projectFiles;
    private String templatePath;

    public SingleDocEval(String templateName) {
        this.templateName = templateName;
    }

    public void setProjectFiles(String[] projectFiles) {
        this.projectFiles = projectFiles;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public static void main(String[] args) throws Exception {
        String textFile = "C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents\\TRT_OPS_RECORD\\material\\text.txt";
        SingleDocEval singleDocEval = new SingleDocEval("TRT_OPS_RECORD");
        singleDocEval.setTemplatePath("C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents\\TRT_OPS_RECORD\\template");
        singleDocEval.setProjectFiles(
                new String[]{"C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents\\TRT_OPS_RECORD\\projects\\project_trt_ops_record.xml"});

        singleDocEval.evalDoc(textFile);
    }

    void evalDoc(String docFile) throws Exception {
        DocumentAnalyzer analyzer = new DocumentAnalyzer(new char[]{':', ' '});

        TemplateTrainer templateTrainer = new TemplateTrainer(analyzer, templatePath);

        System.out.println("Start to eval document from " + docFile);
        String docContent = Files.toString(new File(docFile), Charset.defaultCharset());

        Template trained = templateTrainer.train(docContent, templateName, projectFiles);

        Jigsaw jigsaw = new Jigsaw();
        JigsawResult result = jigsaw.parseDocument(trained, docContent);
        Map<String, String> values = result.getResultValues();

        int expectPartCount = 0;
        for (Label label : trained.getLabelMap().values()) {
            if (label.getType() != ValueType.complex && label.getType() != ValueType.exclude) {
                expectPartCount++;
            }
        }

        System.out.println("==========================DOCUMENT===========================");
        System.out.println("Document:\n" + docContent + "\n");
        System.out.println("==========================PARSED=============================");
        System.out.println("Results:\n" + formatValues(trained, values) + "\n");
        System.out.println("==========================SUMMARY============================");
        System.out.println("ExpectFieldsCount: " + expectPartCount);
        System.out.println("ParsedFieldsCount: " + result.getParsedPartCount());
        System.out.println("FieldCoverRate: " + (float) result.getParsedPartCount() / (float) expectPartCount);
        System.out.println("TextCoverRate: " + result.calculateCoverRate());
        System.out.println("==============================================================");
    }

    public String formatValues(Template template, Map<String, String> results) {
        Map<String, Label> labelMap = template.getLabelMap();
        StringBuilder formatValue = new StringBuilder("{\n");
        for (Map.Entry<String, String> resultEntry : results.entrySet()) {
            Label label = labelMap.get(resultEntry.getKey());
            if (label != null) {
                formatValue.append(String.format("\t\"%s\" : \"%s\", \n", label.getDesc(), resultEntry.getValue()));
            }
        }
        formatValue.append("}");
        return formatValue.toString();
    }
}