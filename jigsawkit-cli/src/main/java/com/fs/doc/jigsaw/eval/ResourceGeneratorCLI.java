package com.fs.doc.jigsaw.eval;

import io.airlift.airline.Cli;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.Option;

import java.io.IOException;

public class ResourceGeneratorCLI {

    public static void main(String[] args) {
        Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("generate_source")
                .withDescription("generate jigsaw resources")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class,
                        GenerateAllResourceCommand.class,
                        GenerateTemplateCommand.class,
                        GenerateProjectCommand.class);

        Cli<Runnable> generateParser = builder.build();

        generateParser.parse(args).run();
    }

    @Command(name = "all", description = "Add file contents to the index")
    public static class GenerateAllResourceCommand implements Runnable {

        @Option(name = "-p", description = "Document base path")
        public String basePath;

        @Option(name = "-t", description = "Template name")
        public String templateName;

        @Option(name = "-d", description = "Template name")
        public String defFile;

        @Override
        public void run() {
            ResourceGenerator generator = new ResourceGenerator();
            try {
                generator.generateAll(basePath, templateName, defFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Command(name = "template", description = "Add file contents to the index")
    public static class GenerateTemplateCommand implements Runnable {
        @Option(name = "-p", description = "Document base path")
        public String basePath;

        @Option(name = "-t", description = "Template name")
        public String templateName;

        @Option(name = "-d", description = "Template name")
        public String defFile;

        @Override
        public void run() {
            ResourceGenerator generator = new ResourceGenerator();

            try {
                generator.generateTemplate(basePath, templateName, defFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Command(name = "project", description = "Add file contents to the index")
    public static class GenerateProjectCommand implements Runnable {
        @Option(name = "-p", description = "Document base path")
        public String basePath;

        @Option(name = "-t", description = "Template name")
        public String templateName;

        @Option(name = "-d", description = "Template name")
        public String defFile;

        @Option(name = "-n", description = "Project name")
        public String projectName;

        @Override
        public void run() {
            ResourceGenerator generator = new ResourceGenerator();

            try {
                generator.generateProjectFile(basePath, projectName, templateName, defFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
