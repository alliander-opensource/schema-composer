package com.alliander.schema_composer;

import com.alliander.schema_composer.generators.AvroGenerator;
import com.alliander.schema_composer.generators.JsonSchemaGenerator;
import com.alliander.schema_composer.generators.OkapiTemplateGenerator;
import io.javalin.http.Handler;
import com.alliander.schema_composer.loaders.EnterpriseArchitectLoader;
import com.alliander.schema_composer.loaders.OntologyLoader;
import org.json.JSONObject;

public class SchemaComposerController {
    public static Handler loadOntology = ctx -> {
        OntologyLoader loader = new OntologyLoader(ctx.pathParam(":url"));
        ctx.result(loader.getResult());
    };

    public static Handler convertToAVro = ctx -> {
        AvroGenerator generator = new AvroGenerator(new JSONObject(ctx.body()));
        ctx.result(generator.generate(false));
    };

    public static Handler convertToJSONSchema = ctx -> {
        JsonSchemaGenerator generator = new JsonSchemaGenerator(new JSONObject(ctx.body()));
        ctx.result(generator.generate());
    };

    public static Handler convertToOkapiTemplate = ctx -> {
        OkapiTemplateGenerator generator = new OkapiTemplateGenerator(new JSONObject(ctx.body()));
        ctx.result(generator.generate());
    };

    public static Handler getDiagrams = ctx -> {
        EnterpriseArchitectLoader loader = new EnterpriseArchitectLoader(new JSONObject(ctx.body()));
        ctx.result(loader.getDiagrams(ctx.pathParam(":pk_guid"), ctx.pathParam(":uid")));
    };

    public static Handler getDiagram = ctx -> {
        EnterpriseArchitectLoader loader = new EnterpriseArchitectLoader(new JSONObject(ctx.body()));
        ctx.result(loader.getDiagram(ctx.pathParam(":dg_guid"), ctx.pathParam(":uid")));
    };

    public static Handler getUserIdentifier = ctx -> {
        EnterpriseArchitectLoader loader = new EnterpriseArchitectLoader(new JSONObject(ctx.body()));
        ctx.result(loader.getUID());
    };
}
