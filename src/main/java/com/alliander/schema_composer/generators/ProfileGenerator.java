package com.alliander.schema_composer.generators;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.io.ByteArrayOutputStream;

public class ProfileGenerator {
    private JSONObject data;
    private JSONArray dataProperties;
    private JSONArray objectProperties;
    private JSONArray inheritance;
    private JSONArray classes;
    private JSONObject enums;
    OWLOntology ontology;
    OWLOntologyManager manager;

    public ProfileGenerator(JSONObject data) {
        this.data = data;
        this.dataProperties = this.data.getJSONObject("axioms").getJSONArray("dataProperties");
        this.objectProperties = this.data.getJSONObject("axioms").getJSONArray("objectProperties");
        this.inheritance = this.data.getJSONObject("axioms").getJSONArray("inheritance");
        this.classes = this.data.getJSONObject("axioms").getJSONArray("classes");
        this.enums = this.data.getJSONObject("axioms").getJSONObject("enums");
        this.dataProperties = this.data.getJSONObject("axioms").getJSONArray("dataProperties");
        this.objectProperties = this.data.getJSONObject("axioms").getJSONArray("objectProperties");
        this.manager = OWLManager.createOWLOntologyManager();
    }

    private OWLDataFactory getFactory() {
        return this.manager.getOWLDataFactory();
    }

    public String generate() throws OWLOntologyCreationException, OWLOntologyStorageException {
        // setup 501 prof header
        this.ontology = this.manager.createOntology();
        this.addOntologyAnnotation("http://www.w3.org/ns/dcat#theme", "profile");
        this.addOntologyAnnotation("http://purl.org/dc/terms/conformsTo", "urn:iso:std:iec:61970-501:draft:ed-2");
        this.addOntologyAnnotation("http://purl.org/dc/terms/language", "en-GB");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        this.manager.saveOntology(this.ontology, new RDFXMLDocumentFormat(), stream);

        OWLNamedIndividual vocab = this.getFactory().getOWLNamedIndividual("http://w3id.org/netbeheer/profilevocab");
        OWLClassAssertionAxiom assertion = this.getFactory().getOWLClassAssertionAxiom(this.getFactory().getOWLClass("test"), vocab);
        this.ontology.add(assertion);
        System.out.println(stream.toString());
        return stream.toString();
    }

    private void addOntologyAnnotation(String prop, String value) {
        OWLAnnotation annotation = this.getFactory().getOWLAnnotation(this.getFactory().getOWLAnnotationProperty(prop), this.getFactory().getOWLLiteral(value));
        this.manager.applyChange(new AddOntologyAnnotation(this.ontology, annotation));
    }
}
