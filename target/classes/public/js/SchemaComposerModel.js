/** demo representation taking into account the current problems */
var schemaDefinedClasses = [];
// represented as { class, property, type, cardinality } (loading an ontology is required to resolve enum definitions)
var schemaDefinedDataProperties = [];
// represented as { class, property, class, cardinality }
var schemaDefinedObjectProperties = [];
// represented as { subClass, superClass }
var schemaDefinedInheritance = [];
// enums
var schemaDefinedEnums = {};

// mapping { guid : name }
var nameMapping = {}

function addClassInternal(iri) {
    if (schemaDefinedClasses.indexOf(iri) == -1) {
        schemaDefinedClasses.push(iri);
        return true;
    }
    return false;
}

function addAttributeInternal(domain, range, property, minCardinality, maxCardinality) {
    // add the property object
    var propObject = {
        "domain" : domain,
        "range" : range,
        "property" : property,
        "minCardinality" : minCardinality,
        "maxCardinality" : maxCardinality
    }
    for (var x = 0; x < schemaDefinedDataProperties.length; x ++) {
        if (JSON.stringify(schemaDefinedDataProperties[x]) === JSON.stringify(propObject))
            return false;
    }
    schemaDefinedDataProperties.push(propObject);
    return propObject;
}

function addAssociationInternal(domain, range, property, minCardinality, maxCardinality) {
    var propObject = {
        "domain" : domain,
        "range" : range,
        "property" : property,
        "minCardinality" : minCardinality,
        "maxCardinality" : maxCardinality
    }
    for (var x = 0; x < schemaDefinedObjectProperties.length; x++) {
        if (JSON.stringify(schemaDefinedObjectProperties[x]) === JSON.stringify(propObject))
            return false;
    }
    schemaDefinedObjectProperties.push(propObject);
    return propObject;
}

function addInheritanceInternal(subClass, superClass) {
    var inheritanceObject = {
        "subClass" : subClass,
        "superClass" : superClass
    }
    for (var x = 0; x < schemaDefinedInheritance.length; x++) {
        if (JSON.stringify(schemaDefinedInheritance[x]) === JSON.stringify(inheritanceObject))
            return false;
    }
    schemaDefinedInheritance.push(inheritanceObject);
    return inheritanceObject;
}

function deleteInheritanceInternal(inherit) {
    var inherit = JSON.parse(JSON.stringify(inherit));
    var toKeep = [];
    for (var x = 0; x < schemaDefinedInheritance.length; x++) {
        if (!(schemaDefinedInheritance[x]['subClass'] == inherit['subClass'] && schemaDefinedInheritance[x]['superClass'] == inherit['superClass'])) {
            toKeep.push(schemaDefinedInheritance[x]);
        }
    }
    schemaDefinedInheritance = toKeep;
}

function deleteAssociationInternal(ass) {
    var toKeep = [];
    for (var x = 0; x < schemaDefinedObjectProperties.length; x++) {
        if (!(schemaDefinedObjectProperties[x]['domain'] == ass['domain'] && schemaDefinedObjectProperties[x]['range'] == ass['range'] && schemaDefinedObjectProperties[x]['property'] == ass['property'])) {
            toKeep.push(schemaDefinedObjectProperties[x]);
        }
    }
    schemaDefinedObjectProperties = toKeep;
}

function deleteAttributeInternal(attribute) {
    var att = JSON.parse(JSON.stringify(attribute));
    var toKeep = [];
    for (var x = 0; x < schemaDefinedDataProperties.length; x++) {
        if (!(schemaDefinedDataProperties[x]['domain'] == att['domain'] && schemaDefinedDataProperties[x]['range'] == att['range'] && schemaDefinedDataProperties[x]['property'] == att['property'])) {
            toKeep.push(schemaDefinedDataProperties[x]);
        }
    }
    schemaDefinedDataProperties = toKeep;
}