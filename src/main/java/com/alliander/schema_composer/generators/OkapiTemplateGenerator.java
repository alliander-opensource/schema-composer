package com.alliander.schema_composer.generators;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class OkapiTemplateGenerator {
    private JSONObject data;
    private JSONArray dataProperties;
    private JSONArray objectProperties;
    private JSONArray inheritance;
    private JSONArray classes;
    private JSONObject enums;

    public OkapiTemplateGenerator(JSONObject data) {
        this.data = data;
        this.dataProperties = this.data.getJSONObject("axioms").getJSONArray("dataProperties");
        this.objectProperties = this.data.getJSONObject("axioms").getJSONArray("objectProperties");
        this.inheritance = this.data.getJSONObject("axioms").getJSONArray("inheritance");
        this.classes = this.data.getJSONObject("axioms").getJSONArray("classes");
        this.enums = this.data.getJSONObject("axioms").getJSONObject("enums");
    }

    public OkapiTemplateGenerator(String filePath) throws IOException {
        this(new JSONObject(new String(Files.readAllBytes(Paths.get(filePath)))));
    }

    public String generate() {
        return "OKAPI output here";
    }
}
