var loadedOntologies = [];
var loadedIRIs = [];

var uniqueClasses = [];
var uniqueDataProperties = [];
var uniqueObjectProperties = [];

function loadOntology() {
    // if using IRI
    var iri = document.getElementById("IRI").value;
    loadOntologyFromIRI(iri);
}

function loadOntologyFromIRI(iri) {
    if (loadedIRIs.indexOf(iri) == -1) {
        var encodedIri = encodeURIComponent(iri);
        var xhttp = new XMLHttpRequest();
        xhttp.open("GET", "/LoadOntology/" + encodedIri, true);
        xhttp.send();
        xhttp.onreadystatechange = function() {
            console.log("done loading");
            if (this.readyState == 4 && this.status == 200) {
                console.log("done loading succes");
                var obj = JSON.parse(this.responseText);
                if (obj["loaded"]) {
                    loadedOntologies.push(obj);
                    loadedIRIs.push(iri);
                    document.getElementById("LoadedOntologies").innerHTML += "<li>" + iri + "</li>";
                    updateLoadedAssertions();
                }
            }
        };
    }
}

function updateLoadedAssertions() {
    // iterate over the array
    var temp = [];
    var tempDataProperties = [];
    var tempObjectProperties = [];
    for (var i = 0; i < loadedOntologies.length; i++) {
        var classes = loadedOntologies[i]["payload"]["classDeclarations"];
        var dataProperties = loadedOntologies[i]["payload"]["dataPropertyDeclarations"];
        var objectProperties = loadedOntologies[i]["payload"]["objectPropertyDeclarations"];
        for (k = 0; k < dataProperties.length; k++) {
            if (tempDataProperties.indexOf(dataProperties[k]) == -1) {
                tempDataProperties.push(dataProperties[k]);
            }
        }

        for (l = 0; l < objectProperties.length; l++) {
            if (tempObjectProperties.indexOf(objectProperties[l]) == -1) {
                tempObjectProperties.push(objectProperties[l]);
            }
        }

        for (j = 0; j < classes.length; j++) {
            if (temp.indexOf(classes[j]) == -1) {
                temp.push(classes[j]);
            }
        }
    }
    uniqueObjectProperties = tempObjectProperties;
    uniqueDataProperties = tempDataProperties;
    uniqueClasses = temp;
}

function getClass(name) {
    var classes = [];
    for (x = 0; x < uniqueClasses.length; x++) {
        if (uniqueClasses[x].toLowerCase().indexOf(name.toLowerCase()) !== -1) {
            classes.push(uniqueClasses[x]);
        }
    }
    return classes;
}

function getRootClass(name) {
    var classes = [];
    for (x = 0; x < schemaDefinedClasses.length; x++) {
        if (schemaDefinedClasses[x].toLowerCase().indexOf(name.toLowerCase()) !== -1) {
            classes.push(schemaDefinedClasses[x]);
        }
    }
    return classes;
}

function getDataProperties(name) {
    var properties = [];
    for (x = 0; x < uniqueDataProperties.length; x++) {
        if (uniqueDataProperties[x].toLowerCase().indexOf(name.toLowerCase()) !== -1) {
            properties.push(uniqueDataProperties[x]);
        }
    }
    return properties;
}

function getObjectProperties(name) {
    var objectProperties = [];
    for (x = 0; x < uniqueObjectProperties.length; x++) {
        if (uniqueObjectProperties[x].toLowerCase().indexOf(name.toLowerCase()) !== -1) {
            objectProperties.push(uniqueObjectProperties[x]);
        }
    }
    return objectProperties;
}

function getAttributeRange(attr) {
    var ranges = [];
    for (var i = 0; i < loadedOntologies.length; i++) {
        if (loadedOntologies[i]["payload"]["dataPropertyTypes"][attr] !== undefined) {
            var ran = loadedOntologies[i]["payload"]["dataPropertyTypes"][attr];
            for (var j = 0; j < ran.length; j++) {
                if (ranges.indexOf(ran) == -1) {
                    ranges.push(ran);
                }
            }
        }
    }
    return ranges;
}