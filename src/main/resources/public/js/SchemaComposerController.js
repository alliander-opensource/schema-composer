// Controller functions to ADD classes, attributes, associations and inheritance
// *****************************************************************************

function addClass() {
    var split = document.getElementById("ClassInput").value.split("#");
    if (!document.getElementById("ClassInput").value.includes("#")) {
        split = document.getElementById("ClassInput").value.split("/");
    }
    var iri = split[split.length-1].split(">")[0];
    if (addClassInternal(iri)) {
        addClassToCanvas(iri, [], iri);
    }
    document.getElementById("ClassInput").value = "";
}

function addAttribute() {
    var split = document.getElementById("ClassAttribute").value.split("#");
    if (!document.getElementById("ClassAttribute").value.includes("#")) {
        split = document.getElementById("ClassAttribute").value.split("/");
    }
    var ranges = getAttributeRange(document.getElementById("ClassAttribute").value);
    var type = " : undefined"
    if (ranges.length > 0) {
        type = " : " + ranges[0];
    }
    type = " : " + document.getElementById("ClassAttributeRange").value;
    var cardinality = " [" + document.getElementById("ClassAttributeMin").value + ".." + document.getElementById("ClassAttributeMax").value + "]";
    var range = "undefined";
    if (ranges.length > 0)
        range = ranges[0] + "";
    range = document.getElementById("ClassAttributeRange").value;
    if (addAttributeInternal(canvas.getActiveObject().id, range, document.getElementById("ClassAttribute").value, document.getElementById("ClassAttributeMin").value, document.getElementById("ClassAttributeMax").value)) {
        addAttributeToCanvas(split[split.length-1].split(">")[0] + type + cardinality);
    }
    document.getElementById("ClassAttribute").value = "";
    document.getElementById("ClassAttributeMin").value = "1";
    document.getElementById("ClassAttributeMax").value = "1";
    document.getElementById("ClassAttributeRange").value = "";
}

function addAssociation() {
    var split = document.getElementById("ClassRelation").value.split("#");
    if (!document.getElementById("ClassRelation").value.includes("#")) {
        split = document.getElementById("ClassRelation").value.split("/");
    }
    var label = split[split.length-1].split(">")[0] + " [" + document.getElementById("ClassRelationMin").value + ".." + document.getElementById("ClassRelationMax").value + "]";
    var propObject = addAssociationInternal(canvas.getActiveObject().id, document.getElementById("ClassRelationRange").value, document.getElementById("ClassRelation").value, document.getElementById("ClassRelationMin").value, document.getElementById("ClassRelationMax").value);
    if (propObject) {
        addAssociationToCanvas(document.getElementById("ClassRelationRange").value, label, propObject);
        document.getElementById("ClassRelation").value = "";
        document.getElementById("ClassRelationRange").value = "";
        document.getElementById("ClassRelationMin").value = "1";
        document.getElementById("ClassRelationMax").value = "1";
    }
}

function addInheritance() {
    var subClass = canvas.getActiveObject().id;
    var superClass = document.getElementById("SuperClass").value;
    var propObject = addInheritanceInternal(subClass, superClass);
    if (propObject) {
        addInheritanceToCanvas(subClass, superClass, propObject);
    }
    document.getElementById("SuperClass").value = "";
}

// Controller functions to DELETE classes, attributes, associations and inheritance
// ********************************************************************************

function deleteInheritance(inherit) {
    deleteInheritanceInternal(inherit);
    deleteInheritanceFromCanvas(inherit);
}

function deleteAssociation(assoc) {
    var ass = JSON.parse(JSON.stringify(assoc));
    deleteAssociationInternal(ass);
    deleteAssociationFromCanvas(ass);
}

function deleteAttribute(attribute) {
    deleteAttributeInternal(attribute);
    resetAttributesOnCanvas();
}

function deleteClass() {
    schemaDefinedClasses = schemaDefinedClasses.filter(e => e !== canvas.getActiveObject().id);
    schemaDefinedDataProperties = schemaDefinedDataProperties.filter(e => e['domain'] !== canvas.getActiveObject().id);
    var relevantAssocs = schemaDefinedObjectProperties.filter(e => e['domain'] == canvas.getActiveObject().id || e['range'] == canvas.getActiveObject().id);
    var relevantInherits = schemaDefinedInheritance.filter(e => e['subClass'] == canvas.getActiveObject().id || e['superClass'] == canvas.getActiveObject().id);
    schemaDefinedObjectProperties = schemaDefinedObjectProperties.filter(e => e['domain'] !== canvas.getActiveObject().id && e['range'] !== canvas.getActiveObject().id);
    schemaDefinedInheritance = schemaDefinedInheritance.filter(e => e['subClass'] !== canvas.getActiveObject().id && e['superClass'] !== canvas.getActiveObject().id);
    for (var x = 0; x < relevantAssocs.length; x++) {
        deleteAssociationFromCanvas(relevantAssocs[x]);
    }
    for (x = 0; x < relevantInherits.length; x++) {
        deleteInheritanceFromCanvas(relevantInherits[x]);
    }
    canvas.remove(canvas.getActiveObject());
}

// Controller functions to display lists of classes, attributes, associations and inheritance
// ******************************************************************************************

document.addEventListener('DOMContentLoaded', function(){
    // event listeners on input fields
    document.getElementById("ClassInput").addEventListener("input", e => getInputList(e, document.getElementById("ClassList"), getClass(e.target.value)));
    document.getElementById("SuperClass").addEventListener("input", e => getInputList(e, document.getElementById("SuperClassList"), getRootClass(e.target.value)));
    document.getElementById("RootClass").addEventListener("input", e => getInputList(e, document.getElementById("RootClassList"), getRootClass(e.target.value)));
    document.getElementById("ClassRelationRange").addEventListener("input", e => getInputList(e, document.getElementById("ClassRelationRangeList"), getRootClass(e.target.value)));
    document.getElementById("ClassRelation").addEventListener("input", e => getInputList(e, document.getElementById("ClassRelationList"), getObjectProperties(e.target.value)));
    document.getElementById("ClassAttribute").addEventListener("input", function(e) {
        document.getElementById("ClassAttributeList").innerHTML = "";
        var dataProperties = getDataProperties(document.getElementById("ClassAttribute").value);
        for (x = 0; x < dataProperties.length; x++) {
            var split = dataProperties[x].split("#");
            if (!dataProperties[x].includes("#")) {
                split = dataProperties[x].split("/");
            }
            var li = document.createElement("li");
            li.setAttribute("dataIRI", dataProperties[x]);
            li.addEventListener("click", function(e) {
                var split = this.getAttribute("dataIRI").split("#");
                if (!this.getAttribute("dataIRI").includes("#")) {
                    split = this.getAttribute("dataIRI").split("/");
                }
                document.getElementById("ClassAttributeList").innerHTML = "";
                document.getElementById("ClassAttribute").value = this.getAttribute("dataIRI");

                 var ranges = getAttributeRange(document.getElementById("ClassAttribute").value);
                 if (ranges.length > 0) {
                    document.getElementById("ClassAttributeRange").value = ranges[0];
                 }
            });
            li.innerHTML = split[split.length-1].split(">")[0];
            document.getElementById("ClassAttributeList").appendChild(li);
        }
    });

    document.addEventListener("click", function (e) {
        document.getElementById("ClassList").innerHTML = "";
        document.getElementById("ClassAttributeList").innerHTML = "";
        document.getElementById("ClassRelationList").innerHTML = "";
        document.getElementById("RootClassList").innerHTML = "";
        document.getElementById("ClassRelationRangeList").innerHTML = "";
        document.getElementById("SuperClassList").innerHTML = "";
    });

}, false);

// Functions for creating the selection lists of classes, attributes, relations and root class
function getInputList(input, list, listValues) {
    list.innerHTML = "";
    for (x = 0; x < listValues.length; x++) {
        var split = listValues[x].split("#");
        if (!listValues[x].includes("#")) {
            split = listValues[x].split("/");
        }
        var li = document.createElement("li");
        li.setAttribute("value", listValues[x]);
        li.addEventListener("click", function(e) {
            input.target.value = this.getAttribute("value");
            list.innerHTML = "";
        });
        li.innerHTML = split[split.length-1].split(">")[0];
        list.appendChild(li);
    }
}