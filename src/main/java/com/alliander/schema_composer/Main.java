package com.alliander.schema_composer;

import com.alliander.schema_composer.generators.AvroGenerator;
import io.javalin.Javalin;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {

        boolean startJavalin = true;
        // add parameter to execute Generator from Jar without starting Javalin
        try {
            String command = args[0];
            String filePath = args[1];
            String content = "";
            try {
                content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
            } catch (IOException e) {
                System.out.println("[main] Error while trying to read the file");
            }

            try {
                AvroGenerator generator = new AvroGenerator(new JSONObject(content));
                System.out.println(generator.generate());
            } catch (Exception e) {
                System.out.println("[main] Error while generating .avsc, please check your logical model");
            }
            startJavalin = false;
        } catch (Exception e) {
            System.out.println("[main] No flags or invalid flags set, starting Javalin SchemaComposer.");
            System.out.println("[main] use -avro -pathToFile to convert to .avsc directly");
        } finally {
            if (startJavalin) {
                Javalin app = Javalin.create(config -> {
                    config.addStaticFiles("/public");
                }).start(7777);

                app.get("/LoadOntology/:url", SchemaComposerController.loadOntology);
                app.post("/ConvertToAvro", SchemaComposerController.convertToAVro);
                app.post("/ConvertToJsonSchema", SchemaComposerController.convertToJSONSchema);
                app.post("/GetDiagrams/:pk_guid/:uid", SchemaComposerController.getDiagrams);
                app.post("/GetDiagram/:dg_guid/:uid", SchemaComposerController.getDiagram);
                app.post("/GetUserIdentifier", SchemaComposerController.getUserIdentifier);
            }
        }
    }
}