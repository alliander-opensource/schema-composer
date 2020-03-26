package com.alliander.schema_composer.generators;

import org.ainslec.picocog.PicoWriter;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generator for AVRO schema's.
 */
public class AvroGenerator {

    private JSONObject data;
    private String rootClassIRI;
    private String namespace;
    private JSONArray dataProperties;
    private JSONArray objectProperties;
    private JSONArray properties;
    private JSONArray inheritance;
    private JSONArray classes;
    private JSONArray annotations;
    private JSONObject enums;
    private List<String> types;
    private PicoWriter writer;

    /**
     * Instantiates an AvroGenerator using a SchemaComposerModel schema
     * @param data JSON representation of the created SchemaComposerModel schema
     */
    public AvroGenerator(JSONObject data) {
        this.data = data;
        this.rootClassIRI = this.data.get("root").toString();
        this.namespace = this.data.get("namespace").toString();
        this.dataProperties = this.data.getJSONObject("axioms").getJSONArray("dataProperties");
        this.objectProperties = this.data.getJSONObject("axioms").getJSONArray("objectProperties");
        List<Object> properties = this.dataProperties.toList();
        properties.addAll(this.objectProperties.toList());
        this.properties = new JSONArray(properties);
        this.inheritance = this.data.getJSONObject("axioms").getJSONArray("inheritance");
        this.classes = this.data.getJSONObject("axioms").getJSONArray("classes");
        this.enums = this.data.getJSONObject("axioms").getJSONObject("enums");
        this.annotations = this.data.getJSONObject("axioms").getJSONArray("annotations");
        this.types = Arrays.asList("string", "boolean", "int", "long", "float", "double", "bytes", "datetime");
        writer = new PicoWriter();
        writer.writeln("@namespace(\"" + this.namespace + "\")");
        writer.writeln_r("protocol Protocol {");
    }

    public AvroGenerator(String filePath) throws IOException {
        this(new JSONObject(new String(Files.readAllBytes(Paths.get(filePath)))));
    }

    /**
     * Generates an AVRO schema
     * @return Returns the schema as a JSON string
     */
    public String generate(boolean avdl) {
        String avsc = generateRecord(this.rootClassIRI).toString(true);
        if (!avdl)
            return avsc;
        writer.writeln_l("}");
        return writer.toString();
    }

    private Schema generateRecord(String IRI) {
        SchemaBuilder.FieldAssembler<Schema> recordAssembler = SchemaBuilder.record(getNameFromIRI(IRI)).namespace(this.namespace).fields();
        PicoWriter schemaWriter = writer.createDeferredWriter();
        if (!getAnnotation(IRI).equals("")) {
            recordAssembler = SchemaBuilder.record(getNameFromIRI(IRI)).doc(getAnnotation(IRI)).namespace(this.namespace).fields();
            schemaWriter.writeln("/**" + getAnnotation(IRI) + "*/");
        }
        schemaWriter.writeln_r("record " + IRI + " {");
        List<String> classes = getSuperClasses(IRI);
        for (int x = 0; x < this.properties.length(); x++) {
            JSONObject prop = this.properties.getJSONObject(x);
            if (classes.contains(prop.get("domain"))) {
                PicoWriter propertyWriter = schemaWriter.createDeferredWriter();
                int min = Integer.parseInt(prop.get("minCardinality").toString());
                int max = Integer.parseInt(prop.get("maxCardinality").toString());
                if (getAnnotationInheritance(classes, prop.get("property").toString()).equals("")) {
                    recordAssembler = recordAssembler.name(getNameFromIRI(prop.get("property").toString())).type(getTypeSchema(prop.get("range").toString(), min, max, propertyWriter)).noDefault();
                } else {
                    propertyWriter.writeln("/**" + getAnnotationInheritance(classes, prop.get("property").toString()) + "*/");
                    recordAssembler = recordAssembler.name(getNameFromIRI(prop.get("property").toString())).doc(getAnnotationInheritance(classes, prop.get("property").toString())).type(getTypeSchema(prop.get("range").toString(), min, max, propertyWriter)).noDefault();
                }
                propertyWriter.write(getNameFromIRI(prop.get("property").toString()) + ";");
            }
        }
        schemaWriter.writeln_l("}");
        schemaWriter.writeln("");
        return recordAssembler.endRecord();
    }

    private Schema getTypeSchema(String range, int min, int max, PicoWriter propertyWriter) {
        Schema typeSchema;
        String type = range;
        if (isTypeOfClass(range)) {
            typeSchema = SchemaBuilder.builder().type(generateRecord(range));
        } else if (isTypeOfEnum(range)) {
            typeSchema = SchemaBuilder.builder().type(getEnumSchema(range));
        } else {
            typeSchema = SchemaBuilder.builder().type(mapDatatype(range));
            type = mapDatatype(range);
        }
        // Cardinality
        if (min == 0 && max == 1) {
            propertyWriter.write("union { null, " + type + " } ");
            return SchemaBuilder.unionOf().nullType().and().type(typeSchema).endUnion();
        } else if (min == 0 && max == -1) {
            propertyWriter.write("union { null, array < " + type + " > } ");
            return SchemaBuilder.unionOf().nullType().and().array().items().type(typeSchema).endUnion();
        } else if (min == 1 && max == -1) {
            propertyWriter.write("array < " + type + " >");
            return SchemaBuilder.array().items().type(typeSchema);
        } else {
            propertyWriter.write(type + " ");
            return typeSchema;
        }
    }

    private Schema getEnumSchema(String IRI) {
        PicoWriter enumWriter = writer.createDeferredWriter();
        enumWriter.write("enum " + IRI + " { ");
        List<String> enumElements = new ArrayList<String>();
        JSONArray enumValues = this.enums.getJSONArray(IRI);
        for (int i = 0; i < enumValues.length(); i++) {
            enumWriter.write(enumValues.get(i).toString());
            if (i < enumValues.length()-1)
                enumWriter.write(", ");
            enumElements.add(enumValues.get(i).toString());
        };
        String[] enumElementsArray = enumElements.toArray(new String[0]);
        enumWriter.writeln(" }");
        enumWriter.writeln("");
        return SchemaBuilder.enumeration(IRI).namespace(this.namespace).symbols(enumElementsArray);
    }

    private List<String> getSuperClasses(String subClass) {
        List<String> classList = new ArrayList<String>();
        classList.add(subClass);
        for (int x = 0; x < this.inheritance.length(); x++) {
            if (this.inheritance.getJSONObject(x).get("subClass").equals(subClass)) {
                String superClass = this.inheritance.getJSONObject(x).get("superClass").toString();
                // recursively iterate the inheritance tree
                List<String> superClasses = getSuperClasses(superClass);
                for (int y = 0; y < superClasses.size(); y++) {
                    classList.add(superClasses.get(y));
                }
            }
        }
        return classList;
    }

    private String getNameFromIRI(String IRI) {
        if (IRI.split("#").length > 1) {
            // hacky solution for prefixes
            if (IRI.split("#")[1].contains(".")) {
                return IRI.split("#")[1].split("\\.")[1].split(">")[0];
            }
            return IRI.split("#")[1].split(">")[0];
        }
        return IRI;
    }

    private String mapDatatype(String IRI) {
        for (int x = 0; x < this.types.size(); x++) {
            if (IRI.toLowerCase().contains(this.types.get(x))) {
                if (this.types.get(x).equals("datetime"))
                    return "long";
                return this.types.get(x);
            }
        }
        return "string";
    }

    private boolean isTypeOfClass(String IRI) {
        for (int x = 0; x < this.classes.length(); x++) {
            if (this.classes.get(x).equals(IRI)) {
                return true;
            }
        }
        return false;
    }

    private String getAnnotationInheritance(List<String> IRIs, String propertyIRI) {
        for (int x = 0; x < IRIs.size(); x++) {
            if (!getAnnotation(IRIs.get(x) + "." + propertyIRI).equals(""))
                return getAnnotation(IRIs.get(x) + "." + propertyIRI);
        }
        return "";
    }

    private String getAnnotation(String IRI) {
        for (int x = 0; x < this.annotations.length(); x++) {
            JSONObject annotationObject = this.annotations.getJSONObject(x);
            if (annotationObject.get("iri").equals(IRI))
                return annotationObject.get("annotation").toString();
        }
        return "";
    }

    private boolean isTypeOfEnum(String IRI) {
        return this.enums.has(IRI);
    }
}
