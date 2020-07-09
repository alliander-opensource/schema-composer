package com.alliander.schema_composer.generators;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class ProfileGenerator {
    private JSONObject data;
    private JSONArray dataProperties;
    private JSONArray objectProperties;
    private JSONArray inheritance;
    private JSONArray classes;
    private JSONObject enums;
    private HashMap<String, OWLClass> classList;
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
        this.classList = new HashMap<String, OWLClass>();
        this.manager = OWLManager.createOWLOntologyManager();
    }

    private OWLDataFactory getFactory() {
        return this.manager.getOWLDataFactory();
    }

    public String generate() throws OWLOntologyCreationException, OWLOntologyStorageException {
        JSONObject response = new JSONObject();
        response.put("profile", generateProfile());
        response.put("vocabulary", generateVocabulary());
        return response.toString();
    }

    private String generateVocabulary() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntology ontology = this.manager.createOntology();
        this.addOntologyAnnotation(ontology, "http://purl.org/dc/terms/conformsTo", "urn:iso:std:iec:61970-501:draft:ed-2");
        this.addOntologyAnnotation(ontology, "http://www.w3.org/ns/dcat#theme", "vocabulary");
        this.addOntologyAnnotation(ontology,"http://purl.org/dc/terms/language", "en-GB");

        // classes
        this.classes.forEach(clsName -> {
           OWLClass cls = this.getFactory().getOWLClass(this.cleanIri(clsName.toString()));
           this.classList.put(this.cleanIri(clsName.toString()), cls);
           OWLDeclarationAxiom decl = this.getFactory().getOWLDeclarationAxiom(cls);
           ontology.add(decl);
        });

        // object properties
        this.objectProperties.forEach(prop -> {
            JSONObject propData = (JSONObject) prop;
            OWLObjectProperty oProp = this.getFactory().getOWLObjectProperty(this.cleanIri(propData.getString("property")));
            OWLDeclarationAxiom decl = this.getFactory().getOWLDeclarationAxiom(oProp);
            ontology.add(decl);
        });

        // data properties
        this.dataProperties.forEach(prop -> {
            JSONObject propData = (JSONObject) prop;
            OWLDataProperty dProp = this.getFactory().getOWLDataProperty(this.cleanIri(propData.getString("property")));
            OWLDeclarationAxiom decl = this.getFactory().getOWLDeclarationAxiom(dProp);
            ontology.add(decl);
        });

        this.inheritance.forEach(in -> {
            JSONObject inheritance = (JSONObject) in;
            OWLSubClassOfAxiom subClass = this.getFactory().getOWLSubClassOfAxiom(this.classList.get(this.cleanIri(inheritance.getString("subClass"))), this.classList.get(this.cleanIri(inheritance.getString("superClass"))));
            ontology.add(subClass);
        });

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        RDFXMLDocumentFormat format = new RDFXMLDocumentFormat();
        format.setPrefix("dct", "http://purl.org/dc/terms/");
        this.manager.saveOntology(ontology, format, stream);
        return stream.toString();
    }

    private String generateProfile() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntology ontology = this.manager.createOntology();
        this.addOntologyAnnotation(ontology, "http://www.w3.org/ns/dcat#theme", "profile");
        this.addOntologyAnnotation(ontology, "http://purl.org/dc/terms/conformsTo", "urn:iso:std:iec:61970-501:draft:ed-2");
        this.addOntologyAnnotation(ontology,"http://purl.org/dc/terms/language", "en-GB");

        OWLNamedIndividual vocab = this.getFactory().getOWLNamedIndividual("http://w3id.org/netbeheer/profile#vocab");
        OWLDeclarationAxiom declaration = this.getFactory().getOWLDeclarationAxiom(vocab);
        ontology.add(declaration);

        OWLClass resourceDescriptor = this.getFactory().getOWLClass("http://www.w3.org/ns/dx/prof/ResourceDescriptor");
        OWLClassAssertionAxiom classAssertion = this.getFactory().getOWLClassAssertionAxiom(resourceDescriptor, vocab);
        ontology.add(classAssertion);

        OWLObjectProperty prop = this.getFactory().getOWLObjectProperty("http://www.w3.org/ns/dx/prof/hasRole");
        OWLNamedIndividual vocabRole = this.getFactory().getOWLNamedIndividual("http://www.w3.org/ns/dx/prof/role/vocabulary");
        OWLObjectPropertyAssertionAxiom assertion = this.getFactory().getOWLObjectPropertyAssertionAxiom(prop, vocab, vocabRole);
        ontology.add(assertion);

        OWLObjectPropertyAssertionAxiom formatAssertion = this.getFactory().getOWLObjectPropertyAssertionAxiom(this.getFactory().getOWLObjectProperty("http://purl.org/dc/terms/format"), vocab, this.getFactory().getOWLNamedIndividual("https://www.iana.org/assignments/media-types/application/rdf+xml"));
        ontology.add(formatAssertion);

        OWLObjectPropertyAssertionAxiom resourceAssertion = this.getFactory().getOWLObjectPropertyAssertionAxiom(this.getFactory().getOWLObjectProperty("http://www.w3.org/ns/dx/prof/hasArtifact"), vocab, this.getFactory().getOWLNamedIndividual("http://w3id.org/netbeheer/profilevocab"));
        ontology.add(resourceAssertion);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        RDFXMLDocumentFormat format = new RDFXMLDocumentFormat();
        format.setPrefix("dct", "http://purl.org/dc/terms/");
        this.manager.saveOntology(ontology, format, stream);
        return stream.toString();
    }

    private void addOntologyAnnotation(OWLOntology ontology, String prop, String value) {
        OWLAnnotation annotation = this.getFactory().getOWLAnnotation(this.getFactory().getOWLAnnotationProperty(prop), this.getFactory().getOWLLiteral(value));
        this.manager.applyChange(new AddOntologyAnnotation(ontology, annotation));
    }

    private String cleanIri(String iri) {
        // quick hacky solution TODO fix
        String newIri = iri;
        if (iri.startsWith("<"))
            newIri = newIri.substring(1);
        if (iri.endsWith(">"))
            newIri = newIri.substring(0, newIri.length()-1);
        if (!iri.contains("http"))
            newIri = "http://w3id.org/netbeheer/vocabulary#" + iri;
        return newIri;
    }
}
