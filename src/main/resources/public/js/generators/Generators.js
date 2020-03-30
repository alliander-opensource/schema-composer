function generateAvro() {
    var xhttp = new XMLHttpRequest();
    xhttp.open("POST", "/ConvertToAvro", true);
    var schema = {
        "root" : document.getElementById("RootClass").value,
        "namespace" : document.getElementById("Namespace").value,
        "axioms" : {
            "classes" : schemaDefinedClasses,
            "dataProperties" : schemaDefinedDataProperties,
            "objectProperties" : schemaDefinedObjectProperties,
            "enums" : schemaDefinedEnums,
            "inheritance" : schemaDefinedInheritance,
            "annotations" : schemaDefinedAnnotations
        }
    }
    xhttp.send(JSON.stringify(schema));
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            document.getElementById("Popup").style.display = "Block";
            document.getElementById("PopupOverlay").style.display = "Block";
            console.log(this.responseText);
            var jsonResponse = JSON.parse(this.responseText);
            var pretty = JSON.stringify(jsonResponse, undefined, 2);
            pretty = pretty.replace(/</g, "&lt");
            pretty = pretty.replace(/>/g, "&gt");
            document.getElementById("Schema").innerHTML = pretty;
        }
    };
}

function generateJSONSchema() {
    var xhttp = new XMLHttpRequest();
    xhttp.open("POST", "/ConvertToJsonSchema", true);

    var rootClasses = [];
    var rootClassesValue = document.getElementById("RootClasses").value;
    var rootClassesSplit = rootClassesValue.split(",");
    for (var x = 0; x < rootClassesSplit.length; x++) {
        if (rootClassesSplit[x] != "")
            rootClasses.push(rootClassesSplit[x])
    }
    var schema = {
        "rootClasses" : rootClasses,
        "namespace" : document.getElementById("JSONNamespace").value,
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
            document.getElementById("Popup").style.display = "Block";
            document.getElementById("PopupOverlay").style.display = "Block";
            var jsonResponse = JSON.parse(this.responseText);
            var pretty = JSON.stringify(jsonResponse, undefined, 2);
            pretty = pretty.replace(/</g, "&lt");
            pretty = pretty.replace(/>/g, "&gt");
            document.getElementById("Schema").innerHTML = pretty;
        }
    }
}

function generateOkapiTemplate() {
    var xhttp = new XMLHttpRequest();
    xhttp.open("POST", "/ConvertToOkapiTemplate", true);
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
            document.getElementById("Popup").style.display = "Block";
            document.getElementById("PopupOverlay").style.display = "Block";
            var jsonResponse = JSON.parse(this.responseText);
            var pretty = JSON.stringify(jsonResponse, undefined, 2);
            pretty = pretty.replace(/</g, "&lt");
            pretty = pretty.replace(/>/g, "&gt");
            document.getElementById("Schema").innerHTML = pretty;
        }
    }
}