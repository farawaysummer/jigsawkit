package com.fs.doc.jigsaw.eval;

import com.fs.doc.jigsaw.EmrLabel;
import com.fs.doc.jigsaw.EmrTemplate;
import com.fs.doc.jigsaw.JigsawEmr;
import com.fs.doc.jigsaw.JigsawResult;
import com.fs.doc.jigsaw.extractor.ValueType;
import com.fs.doc.jigsaw.trainer.EmrDocumentAnalyzer;
import com.fs.doc.jigsaw.trainer.EmrTemplateTrainer;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

public class BatchDocEval {
    private final String templateName;
    private String[] projectFiles;
    private String templatePath;
    private boolean onDebug = true;

    private EmrTemplateTrainer templateTrainer;
    private OutputFormat format;

    public BatchDocEval(String templateName) {
        this.templateName = templateName;
    }

    public void setProjectFiles(String[] projectFiles) {
        this.projectFiles = projectFiles;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public void setDebug(boolean onDebug) {
        this.onDebug = onDebug;
    }

    public static void main(String[] args) throws Exception {
        BatchDocEval batchDocEval = new BatchDocEval("INP_DEATH_DISCUSS");
        batchDocEval.setTemplatePath("C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents\\INP_DEATH_DISCUSS\\template");
        batchDocEval.setProjectFiles(
                new String[]{"C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents\\INP_DEATH_DISCUSS\\projects\\project_inp_death_discuss.xml"});

        batchDocEval.init();

        batchDocEval.evalDocs("C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents\\INP_DEATH_DISCUSS\\material\\INP_DEATH_DISCUSS.xml",
                "C:\\starless\\privates\\workspaces\\jigsaw\\env\\documents\\INP_DEATH_DISCUSS\\output\\", 10);
    }

    void init() {
        EmrDocumentAnalyzer analyzer = new EmrDocumentAnalyzer(new char[]{':', ' '});

        templateTrainer = new EmrTemplateTrainer(analyzer, templatePath);

        format = OutputFormat.createPrettyPrint();  //转换成字符串
        format.setEncoding("UTF-8");
    }

    void evalDocs(String docFile, String exportDir, int limit) throws Exception {
        System.out.println("Start to eval documents from " + docFile);

        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }

        SAXReader reader = new SAXReader();
        Document document;
        document = reader.read(new File(docFile));

        int index = 0;
        long startTime = System.currentTimeMillis();
        FileWriter writer = null;
        if (onDebug) {
            writer = new FileWriter(new File(exportDir + "/summary.csv"));
            writer.write("DocId , ExpectFieldsCount , ParsedFieldsCount , FieldCoverRate, TextCoverRate\n");
        }

        List<Element> recordElements = document.getRootElement().elements("RECORD");
        for (Element recordElement : recordElements) {
            Element idElement = recordElement.element("RECORD_ID");
            String docId = idElement.getText();
            Element contentElement = recordElement.element("FCONTENT");
            evalDoc(docId, contentElement.getText(), exportDir, writer);
            index++;

            if (index >= limit) break;
        }

        if (onDebug && writer != null) {
            writer.close();
        }

        System.out.printf("Finished, use time %d ms to process %d documents.\n",
                System.currentTimeMillis() - startTime, index);
        if (onDebug) {
            System.out.printf("The summary result is write to %s", exportDir + "/summary.csv\n");
        }
    }

    private void evalDoc(String docId, String docContent, String exportTo, FileWriter summaryWriter) throws Exception {
        EmrTemplate trained = templateTrainer.train(docContent, templateName, projectFiles);

        if (onDebug) {
            System.out.println("Parsing document " + docId + " .");
        }

        JigsawEmr jigsawEmr = new JigsawEmr();
        JigsawResult result = jigsawEmr.parseDocument(trained, docContent);
        Map<String, String> values = result.getResultValues();
        Document document = DocumentHelper.createDocument();
        document.setXMLEncoding("UTF-8");
        Element rootElement = document.addElement("document");
        Element docElement = rootElement.addElement("doc");
        docElement.addCDATA(docContent);

        Element fieldsElement = rootElement.addElement("fields");

        Map<String, EmrLabel> labelMap = trained.getLabelMap();

        for (Map.Entry<String, String> resultEntry : values.entrySet()) {
            EmrLabel label = labelMap.get(resultEntry.getKey());
            if (label != null) {
                Element fieldElement = fieldsElement.addElement(label.getName());
                fieldElement.addAttribute("desc", label.getDesc());
                fieldElement.setText(resultEntry.getValue());
            }
        }

        if (onDebug) {
            int expectPartCount = 0;
            for (EmrLabel label : trained.getLabelMap().values()) {
                if (label.getType() != ValueType.complex && label.getType() != ValueType.exclude) {
                    expectPartCount++;
                }
            }

            summaryWriter.write(String.format("%s , %d , %d , %.2f, %.2f\n",
                    docId, expectPartCount, result.getParsedPartCount(),
                    (float) result.getParsedPartCount() / (float) expectPartCount,
                    result.calculateCoverRate()));
        }

        String targetFileName = String.format("%s/%s.xml", exportTo, docId);
        FileOutputStream fos = new FileOutputStream(targetFileName);
        XMLWriter writer = new XMLWriter(fos, format);
        writer.write(document);

        writer.close();
    }
}
