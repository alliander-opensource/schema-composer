package com.alliander.schema_composer.loaders;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OntologyLoader {

    private String url;
    private OWLOntologyManager manager;
    private boolean successful;
    private List<OWLClass> classes;
    private List<OWLObjectProperty> objectProperties;
    private List<OWLDataProperty> dataProperties;
    private HashMap<OWLClass, OWLClass> subClassOfAxioms;
    private HashMap<OWLClass, List<OWLObjectProperty>> classObjectProperties;
    private HashMap<OWLClass, List<OWLDataProperty>> classDataProperties;
    private HashMap<OWLObjectProperty, List<OWLClass>> objectPropertyClasses;
    private HashMap<OWLDataProperty, List<OWLDatatype>> dataPropertyTypes;

    public OntologyLoader(String url) {
        this.url = url;
        this.loadOntology();
        this.collectEntities();
        this.collectAxioms();
    }

    private void loadOntology() {
        this.successful = true;
        this.manager = OWLManager.createOWLOntologyManager();
        try {
            this.manager.loadOntology(IRI.create(this.url));
        } catch (Exception e) {
            this.successful = false;
        }
    }

    private void collectEntities() {
        this.classes = new ArrayList<OWLClass>();
        this.objectProperties = new ArrayList<OWLObjectProperty>();
        this.dataProperties = new ArrayList<OWLDataProperty>();
        this.classObjectProperties = new HashMap<OWLClass, List<OWLObjectProperty>>();
        this.classDataProperties = new HashMap<OWLClass, List<OWLDataProperty>>();
        this.objectPropertyClasses = new HashMap<OWLObjectProperty, List<OWLClass>>();
        this.dataPropertyTypes = new HashMap<OWLDataProperty, List<OWLDatatype>>();
        this.manager.ontologies().forEach(ont -> {
           ont.classesInSignature().forEach(cls -> {
               this.classes.add(cls);
               this.classObjectProperties.put(cls, new ArrayList<OWLObjectProperty>());
               this.classDataProperties.put(cls, new ArrayList<OWLDataProperty>());
           });
           ont.objectPropertiesInSignature().forEach(objProp -> {
               this.objectProperties.add(objProp);
               this.objectPropertyClasses.put(objProp, new ArrayList<OWLClass>());
           });
           ont.dataPropertiesInSignature().forEach(datProp -> {
               this.dataProperties.add(datProp);
               this.dataPropertyTypes.put(datProp, new ArrayList<OWLDatatype>());
           });
        });
    }

    private void collectAxioms() {
        this.subClassOfAxioms = new HashMap<OWLClass, OWLClass>();
        this.manager.ontologies().forEach(ont -> {
            // TODO process cardinality
            ont.axioms().forEach(ax -> {
                if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
                    OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) ax;
                    if (subClassOfAxiom.getSubClass().isOWLClass() && subClassOfAxiom.getSuperClass().isOWLClass()) {
                        this.subClassOfAxioms.put(subClassOfAxiom.getSubClass().asOWLClass(), subClassOfAxiom.getSuperClass().asOWLClass());
                    }
                } else if (ax.isOfType(AxiomType.OBJECT_PROPERTY_DOMAIN)) {
                    OWLObjectPropertyDomainAxiom domainAxiom = (OWLObjectPropertyDomainAxiom) ax;
                    domainAxiom.getDomain().classesInSignature().forEach(cls -> {
                        this.classObjectProperties.get(cls).add(domainAxiom.getProperty().asOWLObjectProperty());
                    });
                } else if (ax.isOfType(AxiomType.DATA_PROPERTY_DOMAIN)) {
                    OWLDataPropertyDomainAxiom domainAxiom = (OWLDataPropertyDomainAxiom) ax;
                    domainAxiom.getDomain().classesInSignature().forEach(cls -> {
                       this.classDataProperties.get(cls).add(domainAxiom.getProperty().asOWLDataProperty());
                    });
                } else if (ax.isOfType(AxiomType.OBJECT_PROPERTY_RANGE)) {
                    OWLObjectPropertyRangeAxiom rangeAxiom = (OWLObjectPropertyRangeAxiom) ax;
                    rangeAxiom.getRange().classesInSignature().forEach(cls -> {
                        this.objectPropertyClasses.get(rangeAxiom.getProperty().asOWLObjectProperty()).add(cls);
                    });
                } else if (ax.isOfType(AxiomType.DATA_PROPERTY_RANGE)) {
                    OWLDataPropertyRangeAxiom rangeAxiom = (OWLDataPropertyRangeAxiom) ax;
                    rangeAxiom.getRange().datatypesInSignature().forEach(dt -> {
                        this.dataPropertyTypes.get(rangeAxiom.getProperty().asOWLDataProperty()).add(dt);
                    });
                }
            });
        });
    }

    private <T> JSONArray getJSONArray(List<T> list) {
        JSONArray result = new JSONArray();
        for (T listItem : list) {
            result.put(listItem.toString());
        }
        return result;
    }

    private <S, T> JSONObject getJSONMap(HashMap<S, List<T>> map) {
        JSONObject JSONMap = new JSONObject();
        for (Map.Entry<S, List<T>> entry : map.entrySet()) {
            JSONMap.put(entry.getKey().toString(), getJSONArray(entry.getValue()));
        }
        return JSONMap;
    }

    public String getResult() {
        JSONObject response = new JSONObject();
        response.put("loaded", this.successful);
        response.put("iri", this.url);
        JSONObject payload = new JSONObject();
        payload.put("classDeclarations", getJSONArray(this.classes));
        payload.put("objectPropertyDeclarations", getJSONArray(this.objectProperties));
        payload.put("dataPropertyDeclarations", getJSONArray(this.dataProperties));
        JSONArray inheritance = new JSONArray();
        for (Map.Entry<OWLClass, OWLClass> subClassOf : this.subClassOfAxioms.entrySet()) {
            JSONObject jsonPair = new JSONObject();
            jsonPair.put(subClassOf.getKey().toString(), subClassOf.getValue().toString());
            inheritance.put(jsonPair);
        }
        payload.put("inheritance", inheritance);
        payload.put("classObjectProperties", getJSONMap(this.classObjectProperties));
        payload.put("classDataProperties", getJSONMap(this.classDataProperties));
        payload.put("objectPropertyClasses", getJSONMap(this.objectPropertyClasses));
        payload.put("dataPropertyTypes", getJSONMap(this.dataPropertyTypes));
        response.put("payload", payload);
        return response.toString(4);
    }
}
