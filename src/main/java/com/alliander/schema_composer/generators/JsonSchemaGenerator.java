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
        root.put("$schema", "http://json-schema.org/draft-07/schema#");
        JSONObject properties = new JSONObject();
        JSONObject rootClasses = new JSONObject();
        rootClasses.put("type", "object");
        JSONArray rootClassRefs = new JSONArray();
        if (this.rootClasses.length() == 0)
            this.rootClasses = this.classes;
        // iterate over classes
        this.classes.forEach(cls -> {
            generateDefinition(cls.toString());
        });
        // iterate over the rootclasses
        for (int x = 0; x < this.rootClasses.length(); x++) {
            rootClassRefs.put(new JSONObject().put("$ref", "#/definitions/" + this.rootClasses.get(x)));
        }
        root.put("anyOf", rootClassRefs);
        root.put("definitions", this.definitions);
        return root.toString(4);
    }

    private void generateDefinition(String clsName) {
        // does the definition exist?
        if (!this.definedClasses.contains(clsName)) {
            this.definedClasses.add(clsName);
            JSONObject definition = new JSONObject();
            definition.put("type", "object");
            JSONObject properties = new JSONObject();
            JSONArray required = new JSONArray();

            System.out.println("CLASS: " + clsName);
            // Hack for draft 7, add all inherited properties
            List<String> allSuperClasses = getSuperClasses(clsName, new ArrayList<String>(), true);
            allSuperClasses.add(clsName);
            // add properties
            for (int x = 0; x < this.properties.length(); x++) {
                JSONObject prop = this.properties.getJSONObject(x);
                for (String cls : allSuperClasses) {
                    if (cls.equals(prop.get("domain"))) {
                        JSONObject property = new JSONObject();
                        String type = getType(prop.get("range").toString());

                        String attr = "$ref";
                        Object value = "";
                        List<String> additionalValues = new ArrayList<String>();
                        if (type.equals("enum")) {
                            value = "#/definitions/" + prop.get("range");
                            defineEnum(prop.get("range").toString());
                        } else if (type.equals("class")) {
                            value = "#/definitions/" + prop.get("range");
                            // if object has children put these in the range of the property
                            getSubClasses(prop.getString("range"), additionalValues, true);
                            if (additionalValues.size() > 0) {
                                JSONObject anyOf = new JSONObject();
                                JSONArray values = new JSONArray();
                                for (String a : additionalValues) {
                                    values.put(new JSONObject().put("$ref", "#/definitions/" + a));
                                }
                                values.put(new JSONObject().put("$ref", "#/definitions/" + prop.get("range")));
                                anyOf.put("anyOf", values);
                                value = anyOf;
                            }

                            generateDefinition(prop.get("range").toString());
                        } else {
                            attr = "type";
                            value = type;
                        }

                        int maxCard = Integer.parseInt(prop.get("maxCardinality").toString());
                        if (maxCard == -1 || maxCard > 1) {
                            property.put("type", "array");
                            if (additionalValues.size() > 0)
                                property.put("items", value);
                            else
                                property.put("items", new JSONObject().put(attr, value));
                            property.put("minItems", Integer.parseInt(prop.get("minCardinality").toString()));
                            if (maxCard != -1)
                                property.put("maxItems", maxCard);
                        } else {
                            // check if its required and on the domain of the class itself or domain of parent
                            boolean inDomain = false;
                            for (String c : allSuperClasses) {
                                if (c.equals(prop.get("domain")))
                                    inDomain = true;
                            }

                            if ((prop.getInt("minCardinality") == 1) && inDomain) {
                                required.put(prop.getString("property"));
                            }
                            if (additionalValues.size() == 0)
                                property.put(attr, value);
                        }
                        if (additionalValues.size() > 0 && !(maxCard == -1 || maxCard > 1))
                            properties.put(prop.get("property").toString(), value);
                        else
                            properties.put(prop.get("property").toString(), property);
                    }
                }
            }
            this.definitions.put(clsName, definition);
            if (required.length() > 0)
                definition.put("required", required);
            definition.put("additionalProperties", false);
            definition.put("properties", properties);
        }
    }

    private void defineEnum(String name) {
        JSONObject jsonEnum = new JSONObject();
        jsonEnum.put("type", "string");
        jsonEnum.put("enum", this.enums.getJSONArray(name));
        this.definitions.put(name, jsonEnum);
    }

    /**
     * @param cls name of the class
     * @param up true: get the list of superclasses false: get the list of subclasses
     * @return the list of sub or super classes
     */
    private List<String> getInheritanceClasses(String cls, boolean up) {
        List<String> classList = new ArrayList<String>();
        String target = "superClass";
        String current = "subClass";
        if (!up) {
            target = "subClass";
            current = "superClass";
        }
        for (Object in : this.inheritance) {
            JSONObject inherit = (JSONObject) in;
            if (inherit.get(current).equals(cls)) {
                String inheritClass = inherit.getString(target);
                for (String iCls : getInheritanceClasses(inheritClass, up)) {
                    if (!classList.contains(iCls))
                        classList.add(iCls);
                }
            }
        }
        return classList;
    }

    private List<String> getSuperClasses(String subClass, List<String> classList, boolean recursive) {
        for (int x = 0; x < this.inheritance.length(); x++) {
            if (this.inheritance.getJSONObject(x).get("subClass").equals(subClass)) {
                String superClass = this.inheritance.getJSONObject(x).get("superClass").toString();
                if (recursive)
                    getSuperClasses(superClass, classList, true);
                if (!classList.contains(superClass))
                    classList.add(superClass);
            }
        }
        return classList;
    }

    private List<String> getSubClasses(String superClass, List<String> classList, boolean recursive) {
        for (Object in : this.inheritance) {
            JSONObject inherit = (JSONObject) in;
            if (inherit.get("superClass").equals(superClass)) {
                String subClass = inherit.getString("subClass");
                if (recursive)
                    getSubClasses(subClass, classList, true);
                if (!classList.contains(subClass))
                    classList.add(subClass);
            }
        }
        return classList;
    }

    private String getType(String range) {
        for (String type : this.types) {
            if (type.toLowerCase().contains(range.toLowerCase()))
                return type;
            if (range.toLowerCase().contains("float") || range.toLowerCase().contains("double"))
                return "number";
        }

        // check if enum type
        if (this.enums.has(range))
            return "enum";

        // check if type is a class
        for (Object cls : this.classes) {
            if (cls.toString().equals(range))
                return "class";
        }
        return "string";
    }
}
