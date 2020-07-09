function exportToSVG() {
    var a = document.createElement('a');
    var svg = canvas.toSVG().replace(/id="{.*?}"/g, "");
    a.href = 'data:image/svg+xml,'+encodeURIComponent(svg);
    a.download = 'schema.svg';
    a.click();
}

function exportDiagram() {
    // save the positions of classes in the diagram
    var positions = {};
    for (var x = 0; x < schemaDefinedClasses.length; x++) {
        var id = schemaDefinedClasses[x];
        var obj = selectObject(id);
        positions[id] = { "left": obj.left, "top": obj.top};
    }
    for (var key in schemaDefinedEnums) {
        var obj = selectObject(key);
        positions[key] = { "left": obj.left, "top": obj.top};
    }
    var diagram = {
        "axioms" : {
            "classes" : schemaDefinedClasses,
            "dataProperties" : schemaDefinedDataProperties,
            "objectProperties" : schemaDefinedObjectProperties,
            "enums" : schemaDefinedEnums,
            "inheritance" : schemaDefinedInheritance,
            "annotations" : schemaDefinedAnnotations
        },
        "positions" : positions
    };
    var a = document.createElement('a');
    a.href = 'data:text/json;charset=utf-8,'+encodeURIComponent(JSON.stringify(diagram));
    a.download = 'diagram.json';
    a.click();
}

function exportProfile() {
    var xhttp = new XMLHttpRequest();
    xhttp.open("POST", "/GenerateProfile", true);
    var schema = {
        "axioms" : {
            "classes" : schemaDefinedClasses,
            "dataProperties" : schemaDefinedDataProperties,
            "objectProperties" : schemaDefinedObjectProperties,
            "enums" : schemaDefinedEnums,
            "inheritance" : schemaDefinedInheritance
        }
    }
    xhttp.send(JSON.stringify(schema));
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            var jsonResponse = JSON.parse(this.responseText);
            var a = document.createElement('a');
            a.href = 'data:rdf/xml;charset=utf-8,'+encodeURIComponent(decodeURIComponent(jsonResponse["profile"]));
            a.download = 'profile.rdf';
            a.click();

            a.href = 'data:rdf/xml;charset=utf-8,'+encodeURIComponent(decodeURIComponent(jsonResponse["vocabulary"]));
            a.download = 'vocabulary.rdf';
            a.click();
        }
    }
}