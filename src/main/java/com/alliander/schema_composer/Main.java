package com.alliander.schema_composer;

import com.alliander.schema_composer.generators.AvroGenerator;
import com.alliander.schema_composer.generators.OkapiTemplateGenerator;
import org.apache.commons.cli.*;
import io.javalin.Javalin;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        Option avsc = Option.builder("avsc")
                .hasArg()
                .argName("path/filename")
                .desc("Generate an .avsc from a logical model")
                .build();
        Option avdl = Option.builder("avdl")
                .hasArg()
                .argName("path/filename")
                .desc("Generate an .avdl from a logical model")
                .build();
        Option okapi = Option.builder("okapi")
                .hasArg()
                .argName("path/filename")
                .desc("Generate a template for okapi mappings from a logical model")
                .build();
        options.addOption(avsc);
        options.addOption(avdl);
        options.addOption(okapi);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse( options, args);
            if (cmd.hasOption("avsc")) {
                try {
                    AvroGenerator generator = new AvroGenerator(cmd.getOptionValue("avsc"));
                    System.out.println(generator.generate(false));
                } catch (IOException e) {
                    System.out.println("Error while generating .avsc, please check if <"
                            + cmd.getOptionValue("avsc") + "> refers to a valid logical model");
                }
            } else if (cmd.hasOption("avdl")) {
                try {
                    AvroGenerator generator = new AvroGenerator(cmd.getOptionValue("avdl"));
                    System.out.println(generator.generate(true));
                } catch (IOException e) {
                    System.out.println("Error while generating .avdl, please check if <" + cmd.getOptionValue("avdl")
                            + "> refers to a valid logical model");
                }
            } else if (cmd.hasOption("okapi")) {
                try {
                    OkapiTemplateGenerator generator = new OkapiTemplateGenerator(cmd.getOptionValue("okapi"));
                    System.out.println(generator.generate());
                } catch (IOException e) {
                    System.out.println("Error while generating okapi mapping template, please check if <"
                            + cmd.getOptionValue("okapi") + "> refers to a valid logical model");
                }
            } else {
                Javalin app = Javalin.create(config -> {
                    config.addStaticFiles("/public");
                }).start(7777);

                app.get("/LoadOntology/:url", SchemaComposerController.loadOntology);
                app.post("/ConvertToAvro", SchemaComposerController.convertToAVro);
                app.post("/ConvertToJsonSchema", SchemaComposerController.convertToJSONSchema);
                app.post("/ConvertToOkapiTemplate", SchemaComposerController.convertToOkapiTemplate);
                app.post("/GetDiagrams/:pk_guid/:uid", SchemaComposerController.getDiagrams);
                app.post("/GetDiagram/:dg_guid/:uid", SchemaComposerController.getDiagram);
                app.post("/GetUserIdentifier", SchemaComposerController.getUserIdentifier);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("SchemaGenerator", options);
            System.exit(1);
        }
    }
}