var guidNameMapping = [];

function getUserIdentifier() {
    var xhttp = new XMLHttpRequest();
    xhttp.open("POST", "/GetUserIdentifier", true);
    var data = {
        base: document.getElementById("baseURI").value,
        username: document.getElementById("username").value,
        pwd: document.getElementById("password").value
    };
    xhttp.send(JSON.stringify(data));
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            var obj = JSON.parse(this.responseText);
            document.getElementById("uid").value = unpackJSON(obj, ["rdf:RDF", "ss:login", "ss:useridentifier"]);
        }
    };
}

function loadEADiagram() {
    guidNameMapping = [];

    var dg_guid = document.getElementById("dgGuid").value;
    var uid = document.getElementById("uid").value;

    // quering diagram
    var xhttp = new XMLHttpRequest();
    xhttp.open("POST", "/GetDiagram/" + dg_guid + "/" + uid, true);
    var data = {
        base: document.getElementById("baseURI").value,
        username: document.getElementById("username").value,
        pwd: document.getElementById("password").value
    };
    xhttp.send(JSON.stringify(data));
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            var obj = JSON.parse(this.responseText);
            // parsing the reponse
            // TODO refactor difference enums and classes
            for (var x = 0; x < obj.length; x++) {
                // distinguish between classes and enumerations
                var metadata = unpackJSON(obj[x], ["metadata", "rdf:RDF", "oslc_am:Resource"]);
                // enums sometimes only the stereotype is set
                if (metadata["dcterms:type"] === "Enumeration" || unpackJSON(metadata, ["ss:stereotype", "ss:stereotypename", "ss:name"]) === "enumeration") {
                    addEnumToCanvas(metadata["dcterms:title"], [], metadata["dcterms:title"]);
                    var coords = obj[x]["coords"].split(",");
                    setActiveCanvasObject(metadata["dcterms:title"]);
                    var enumItems = [];
                    // iterating over the attributes of the enum
                    var attributes = unpackJSON(obj[x], ["attributes", "rdf:RDF", "ss:features", "ss:attributes", "rdf:Description", "rdfs:member"]);
                    if (attributes) {
                        if (attributes.length === undefined) {
                            enumItems.push(attributes["ss:attribute"]["dcterms:title"]);
                            addAttributeToCanvas(attributes["ss:attribute"]["dcterms:title"]);
                        }
                        for (var d = 0; d < attributes.length; d++) {
                            enumItems.push(attributes[d]["ss:attribute"]["dcterms:title"]);
                            addAttributeToCanvas(attributes[d]["ss:attribute"]["dcterms:title"]);
                        }
                    }
                    schemaDefinedEnums[metadata["dcterms:title"]] = enumItems;
                    var activeObj = canvas.getActiveObject();
                    activeObj.left = parseInt(coords[0]);
                    activeObj.top = parseInt(coords[1]);
                    activeObj.setCoords();
                } else if (metadata["dcterms:type"] === "Class") {
                    // process classes
                    var name = metadata["dcterms:title"];
                    // call model to add to internal representation
                    if (addClassInternal(name)) {
                        // call controller to add visual representation unpack the coordinates
                        var coords = obj[x]["coords"].split(",");
                        addClassToCanvas(name, [], name);
                        // set active class
                        setActiveCanvasObject(name);
                        // process the attributes of the class
                        var attributes = unpackJSON(obj[x], ["attributes", "rdf:RDF", "ss:features", "ss:attributes", "rdf:Description", "rdfs:member"]);
                        nameMapping[obj[x]["guid"]] = name;
                        if (attributes) {
                            // 1 attribute
                            if (attributes.length === undefined) {
                                var attribute = attributes["ss:attribute"];
                                addAttributeInternal(name, attribute["ss:classifiername"], attribute["dcterms:title"], attribute["ss:lowerbound"], attribute["ss:upperbound"]);
                                // add visually
                                addAttributeToCanvas(attribute["dcterms:title"] + " : " + attribute["ss:classifiername"] + " [" + attribute["ss:lowerbound"] + ".." + attribute["ss:upperbound"] + "]");
                            }
                            for (var y = 0; y < attributes.length; y++) {
                                var attribute = attributes[y]["ss:attribute"];
                                addAttributeInternal(name, attribute["ss:classifiername"], attribute["dcterms:title"], attribute["ss:lowerbound"], attribute["ss:upperbound"]);
                                addAttributeToCanvas(attribute["dcterms:title"] + " : " + attribute["ss:classifiername"] + " [" + attribute["ss:lowerbound"] + ".." + attribute["ss:upperbound"] + "]");
                            }
                        }
                        var activeObj = canvas.getActiveObject();
                        activeObj.left = parseInt(coords[0]);
                        activeObj.top = parseInt(coords[1]);
                        activeObj.setCoords();
                    }
                }
            }
            // after adding the classes to the internal and visual model we can process the connectors
            for (var z = 0; z < obj.length; z++) {
                var metadata = unpackJSON(obj[z], ["metadata", "rdf:RDF", "oslc_am:Resource"]);
                if (metadata["dcterms:type"] === "Class") {
                    var links = unpackJSON(obj[z], ["linkedResources", "rdf:Description"]);
                    if (links) {
                        if (links.length === undefined) {
                            processLink(links, obj[z], metadata["dcterms:title"]);
                        }
                        for (var a = 0; a < links.length; a++) {
                            processLink(links[a], obj[z], metadata["dcterms:title"]);
                        }
                    }
                }
            }
        }
    };
}

function processLink(link, obj, name) {
    if (link["dcterms:type"] === "Association" && link["ss:linkdirection"] === "Outgoing") {
        var ltGuid = link["dcterms:identifier"].split("lt_")[1];
        var key = link["rdf:about"];
        var assocs = unpackJSON(obj, ["linkedResources", "ss:linkedresources", "ss:Association"]);
        if (assocs) {
            if (assocs.length === undefined) {
                if ("#" + assocs["rdf:ID"] === key) {
                    processAssoc(name, assocs["rdf:resource"], connectors[ltGuid]);
                }
            } else {
                for (var x = 0; x < assocs.length; x++) {
                    if ("#" + assocs[x]["rdf:ID"] === key) {
                        processAssoc(name, assocs[x]["rdf:resource"], connectors[ltGuid]);
                    }
                }
            }
        }
    } else if (link["dcterms:type"] === "Generalization" && link["ss:linkdirection"] === "Outgoing") {
        var key = link["rdf:about"];
        var inherit = unpackJSON(obj, ["linkedResources", "ss:linkedresources", "ss:Generalization"]);
        if (inherit) {
            if (inherit.length === undefined) {
                if ("#" + inherit["rdf:ID"] === key) {
                    processInherit(name, inherit["rdf:resource"]);
                }
            } else {
                for (var y = 0; y < inherit.length; y++) {
                    if ("#" + inherit[y]["rdf:ID"] === key) {
                        processInherit(name, inherit[y]["rdf:resource"]);
                    }
                }
            }
        }
    }

}

function processInherit(subClass, superClass) {
    setActiveCanvasObject(subClass);
    var guid = "{" + superClass.split("{")[1].split("/")[0];
    var superClassName = nameMapping[guid];
    if (superClassName) {
        document.getElementById("SuperClass").value = superClassName;
        addInheritance();
    }
}

function processAssoc(sourceName, domain, ltData) {
    // hacky now process properly later
    var guid = "{" + domain.split("{")[1].split("/")[0];
    var destName = nameMapping[guid];

    if (destName) {
        // check if there is a source end
        if (ltData["sourceEnd"]) {
            loadAssoc(destName, sourceName, ltData["sourceEnd"]["lowerBound"], ltData["sourceEnd"]["upperBound"], ltData["sourceEnd"]["roleName"]);
        }
        // check if there is a dest end
        if (ltData["destEnd"]) {
            loadAssoc(sourceName, destName, ltData["destEnd"]["lowerBound"], ltData["destEnd"]["upperBound"], ltData["destEnd"]["roleName"]);
        }
    }
}

function loadAssoc(sourceName, destName, minCard, maxCard, roleName) {
    setActiveCanvasObject(sourceName);
    document.getElementById("ClassRelationRange").value = destName;
    document.getElementById("ClassRelation").value = roleName;
    document.getElementById("ClassRelationMin").value = minCard;
    document.getElementById("ClassRelationMax").value = maxCard;
    addAssociation();
}

function unpackJSON(obj, keys) {
    var currentObj = obj;
    for (var x = 0; x < keys.length; x++) {
        if (keys[x] in currentObj) {
            currentObj = currentObj[keys[x]];
        } else {
            return null;
        }
    }
    return currentObj;
}