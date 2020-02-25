package com.alliander.schema_composer.generators;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonSchemaGenerator {

    private JSONObject data;
    private JSONArray rootClasses;
    private String namespace;
    private JSONArray dataProperties;
    private JSONArray objectProperties;
    private JSONArray properties;
    private JSONArray inheritance;
    private JSONArray classes;
    private JSONObject enums;
    private JSONObject definitions;
    private List<String> definedClasses;
    private List<String> types;

    public JsonSchemaGenerator(JSONObject data) {
        this.data = data;
        this.rootClasses = this.data.getJSONArray("rootClasses");
        this.namespace = this.data.get("namespace").toString();
        this.dataProperties = this.data.getJSONObject("axioms").getJSONArray("dataProperties");
        this.objectProperties = this.data.getJSONObject("axioms").getJSONArray("objectProperties");
        List<Object> properties = this.dataProperties.toList();
        properties.addAll(this.objectProperties.toList());
        this.properties = new JSONArray(properties);
        this.inheritance = this.data.getJSONObject("axioms").getJSONArray("inheritance");
        this.classes = this.data.getJSONObject("axioms").getJSONArray("classes");
        this.enums = this.data.getJSONObject("axioms").getJSONObject("enums");
        this.definitions = new JSONObject();
        this.types = Arrays.asList("boolean", "integer", "null", "number", "string");
        this.definedClasses = new ArrayList<String>();
    }

    public String generate() {
        JSONObject root = new JSONObject();
        root.put("$schema", "http://json-schema.org/2019-09/schema#");
        root.put("$id", "namespace/schemaname#");
        root.put("type", "object");

        // build properties
        JSONObject properties = new JSONObject();

        // root classes
        JSONObject rootClasses = new JSONObject();
        rootClasses.put("type", "object");

        JSONArray rootClassRefs = new JSONArray();
        if (this.rootClasses.length() == 0)
            this.rootClasses = this.classes;

        // iterate over the rootclasses and collect the definition objects
        for (int x = 0; x < this.rootClasses.length(); x++) {
            rootClassRefs.put(new JSONObject().put("$ref", "#/$defs/" + this.rootClasses.get(x)));
            generateDefinition(this.rootClasses.get(x).toString());
        }
        rootClasses.put("anyOf", rootClassRefs);

        properties.put("Schemaname", rootClasses);
        root.put("properties", properties);
        root.put("$defs", this.definitions);

        return root.toString(4);
    }

    private void generateDefinition(String clsName) {
        // does the definition exist?
        if (!this.definedClasses.contains(clsName)) {
            this.definedClasses.add(clsName);
            JSONObject definition = new JSONObject();
            definition.put("type", "object");
            // TODO add comments
            JSONObject properties = new JSONObject();

            // add properties
            for (int x = 0; x < this.properties.length(); x++) {
                JSONObject prop = this.properties.getJSONObject(x);
                if (clsName.equals(prop.get("domain"))) {
                    JSONObject property = new JSONObject();
                    String type = getType(prop.get("range").toString());

                    property.put("minItems", Integer.parseInt(prop.get("minCardinality").toString()));
                    int maxCard = Integer.parseInt(prop.get("maxCardinality").toString());
                    if (maxCard != -1)
                        property.put("maxItems", maxCard);

                    String attr = "$ref";
                    String value = "";
                    if (type.equals("enum")) {
                        value =  "#/$defs/" + prop.get("range");
                        defineEnum(prop.get("range").toString());
                    } else if (type.equals("class")) {
                        value = "#/$defs/" + prop.get("range");
                        generateDefinition(prop.get("range").toString());
                    } else {
                       attr = "type";
                       value = type;
                    }

                    if (maxCard == -1 || maxCard > 1) {
                        property.put("type", "array");
                        property.put("items", new JSONObject().put(attr, value));
                    } else {
                        property.put(attr, value);
                    }


                    properties.put(prop.get("property").toString(), property);
                }
            }

            definition.put("properties", properties);

            List<String> superClasses = getSuperClasses(clsName);
            if (superClasses.size() > 0) {
                JSONArray inheritance = new JSONArray();
                for (int x = 0; x < superClasses.size(); x++) {
                    generateDefinition(superClasses.get(x));
                    inheritance.put(new JSONObject().put("$ref", "#/$defs/" + superClasses.get(x)));
                }
                inheritance.put(definition);
                this.definitions.put(clsName, new JSONObject().put("allOf", inheritance));
            } else {
                this.definitions.put(clsName, definition);
            }
        }
    }

    private void defineEnum(String name) {
        JSONObject jsonEnum = new JSONObject();
        jsonEnum.put("type", "string");
        JSONArray values = new JSONArray();
        jsonEnum.put("enum", this.enums.getJSONArray(name));
        this.definitions.put(name, jsonEnum);
    }

    private List<String> getSuperClasses(String subClass) {
        List<String> classList = new ArrayList<String>();
        for (int x = 0; x < this.inheritance.length(); x++) {
            if (this.inheritance.getJSONObject(x).get("subClass").equals(subClass)) {
                String superClass = this.inheritance.getJSONObject(x).get("superClass").toString();
                classList.add(superClass);
            }
        }
        return classList;
    }

    private String getType(String range) {
        for (int x = 0; x < this.types.size(); x++) {
            if (this.types.get(x).toLowerCase().contains(range.toLowerCase())) {
                return this.types.get(x);
            }
        }

        // check if enum type
        if (this.enums.has(range)) { return "enum"; }
        for (int x = 0; x < this.classes.length(); x++) {
            if (this.classes.get(x).toString().equals(range))
                return "class";
        }
        return "string";
    }
}
