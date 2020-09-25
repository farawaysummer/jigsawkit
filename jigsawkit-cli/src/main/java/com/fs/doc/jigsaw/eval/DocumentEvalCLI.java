package com.fs.doc.jigsaw.eval;

import io.airlift.airline.Cli;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.Option;

import java.io.File;

public class DocumentEvalCLI {

    public static void main(String[] args) {
        Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("eval_doc")
                .withDescription("eval document")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class,
                        SingleEvalCommand.class,
                        BatchEvalCommand.class);

        Cli<Runnable> generateParser = builder.build();

        generateParser.parse(args).run();
    }

    @Command(name = "one", description = "Eval one document")
    public static class SingleEvalCommand implements Runnable {
        @Option(name = "-p", description = "Document base path")
        public String templatePath;

        @Option(name = "-o", description = "Project base path")
        public String projectPath;

        @Option(name = "-d", description = "Document file path")
        public String docFilePath;

        @Option(name = "-t", description = "Template name")
        public String templateName;

        @Override
        public void run() {
            SingleDocEval singleDocEval = new SingleDocEval(templateName);
            singleDocEval.setTemplatePath(templatePath);

            File prjDir = new File(projectPath);
            if (!prjDir.exists()) {
                System.err.println("No project directory.");
                return;
            }

            File[] prjFiles = prjDir.listFiles();
            if (prjFiles == null) {
                System.err.println("Can't find any project files.");
                return;
            }

            String[] proFilePaths = new String[prjFiles.length];
            for (int index = 0 ; index < prjFiles.length ;index++) {
                proFilePaths[index] = prjFiles[index].getAbsolutePath();
            }

            singleDocEval.setProjectFiles(proFilePaths);

            try {
                singleDocEval.evalDoc(docFilePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Command(name = "batch", description = "Eval batch documents")
    public static class BatchEvalCommand implements Runnable {

        @Option(name = "-p", description = "Document base path")
        public String basePath;

        @Option(name = "-t", description = "Template name")
        public String templateName;

        @Option(name = "-l", description = "Limit of eval documents")
        public int limit = 0;

        @Option(name = "-d", description = "Set debug mode")
        public boolean debug = true;

        @Override
        public void run() {
            BatchDocEval batchDocEval = new BatchDocEval(templateName);

            if (!basePath.endsWith("/") && basePath.endsWith("\\")) {
                basePath = basePath + "/";
            }

            batchDocEval.setTemplatePath(basePath + templateName + "/template");

            File prjDir = new File(basePath + templateName + "/projects/");
            if (!prjDir.exists()) {
                System.err.println("No project directory.");
                return;
            }

            File[] prjFiles = prjDir.listFiles();
            if (prjFiles == null) {
                System.err.println("Can't find any project files.");
                return;
            }

            String[] proFilePaths = new String[prjFiles.length];
            for (int index = 0 ; index < prjFiles.length ;index++) {
                proFilePaths[index] = prjFiles[index].getAbsolutePath();
            }

            batchDocEval.setProjectFiles(proFilePaths);
            batchDocEval.setDebug(debug);

            batchDocEval.init();

            try {
                File materialFile = new File(basePath + templateName + "/material/");
                if (!materialFile.exists()) {
                    return;
                }

                String[] materialFiles = materialFile.list();
                if (materialFiles == null) {
                    return;
                }

                for (String filePath : materialFiles) {
                    batchDocEval.evalDocs(filePath, basePath + templateName + "/output", limit);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
