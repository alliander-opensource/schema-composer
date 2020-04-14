package com.alliander.schema_composer.generators;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class OkapiTemplateGenerator {
    private JSONObject data;
    private JSONArray dataProperties;
    private JSONArray objectProperties;
    private JSONArray inheritance;
    private JSONArray classes;
    private JSONObject enums;
    private JSONObject result;

    public OkapiTemplateGenerator(JSONObject data) {
        this.data = data;
        this.dataProperties = this.data.getJSONObject("axioms").getJSONArray("dataProperties");
        this.objectProperties = this.data.getJSONObject("axioms").getJSONArray("objectProperties");
        this.inheritance = this.data.getJSONObject("axioms").getJSONArray("inheritance");
        this.classes = this.data.getJSONObject("axioms").getJSONArray("classes");
        this.enums = this.data.getJSONObject("axioms").getJSONObject("enums");
        this.result = new JSONObject();
        this.dataProperties = this.data.getJSONObject("axioms").getJSONArray("dataProperties");
        this.objectProperties = this.data.getJSONObject("axioms").getJSONArray("objectProperties");
    }

    public OkapiTemplateGenerator(String filePath) throws IOException {
        this(new JSONObject(new String(Files.readAllBytes(Paths.get(filePath)))));
    }

    public String generate() {
        // for each classes
        this.classes.forEach(cls-> {
            JSONObject properties = new JSONObject();
            JSONArray relations = new JSONArray();
            this.objectProperties.forEach(oP-> {
                JSONObject prop = (JSONObject) oP;
                JSONObject relationProperties = new JSONObject();
                //check als het objectProperty van de class is
                if (prop.getString("domain").equals(cls.toString())) {
                    relationProperties.put("name", prop.getString("property"));
                    relationProperties.put("from_key", "");
                    relationProperties.put("to_object", prop.getString("range"));
                    relationProperties.put("to_key", "");
                    relations.put(relationProperties);
                }
            });
            properties.put("relations", relations);
            properties.put("base_source", new JSONArray());
            JSONObject mapping = new JSONObject();
            this.dataProperties.forEach(dP -> {
                // check als het een dataproperty van de class is
                JSONObject prop = (JSONObject) dP;
                JSONObject mappingProperties = new JSONObject();
                if (prop.getString("domain").equals(cls.toString())) {
                    mappingProperties.put("source", "");
                    mappingProperties.put("mapper", "");
                    JSONObject config = new JSONObject();
                    config.put("property_keys", new JSONArray());
                    config.put("constant_value", "");
                    mappingProperties.put("mapper_config", config);
                    mapping.put(prop.getString("property"), mappingProperties);
                }
            });
            properties.put("mapping", mapping);
            this.result.put(cls.toString(), properties);
        });        
        return this.result.toString(4);
    }
}
