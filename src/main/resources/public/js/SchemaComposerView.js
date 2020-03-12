var canvas;
var classEditor;
var assocPairs = [];

function addClassToCanvas(name, attributes, iri) {
    var classToAdd = {
        "name": name,
        "attributes": attributes,
        "IRI": iri
    }
    canvas.add(getFabricOwlClass(classToAdd));
}

function addEnumToCanvas(name, attributes, iri) {
    var classToAdd = {
        "name": name,
        "type": "Enum",
        "attributes": attributes,
        "IRI": iri
    }
    canvas.add(getFabricOwlClass(classToAdd));
}

function addAttributeToCanvas(attribute) {
    // empty the group object and redraw the group elements
    var group = canvas.getActiveObject();
    if (group.item(2).text != "") {
        group.item(2).text += "\n";
    }
    var tempWidth = group.width;
    group.item(2).text += "+" + attribute;
    group.dirty = true;
    canvas.renderAll();

    group.addWithUpdate();
    if (tempWidth != group.width) {
        group.item(0).width = group.width + 6;
        group.item(1).width = group.width + 6;
    }
    group.item(0).height = group.height + 6;
    group.addWithUpdate();
    group.dirty = true;
    showClassEditor();
    updateAssociations();
    canvas.renderAll();
}

function addAssociationToCanvas(target, label, propObject) {
    var current = canvas.getActiveObject();
    var target = selectObject(target);
    var assoc = getAssociation(current, target, label, JSON.stringify(propObject));
    canvas.add(assoc);
    assoc.moveTo(0);
    updateAssociation(assoc, current, target, 0, 0, []);
    canvas.renderAll();
    assocPairs.push([assoc, current, target]);
    showClassEditor();
}

function addInheritanceToCanvas(subClass, superClass, propObject) {
    // TODO refactor different object for association and inheritance
    var subClass = selectObject(subClass);
    var superClass = selectObject(superClass);
    var assoc = getAssociation(subClass, superClass, "inherits", JSON.stringify(propObject));
    canvas.add(assoc);
    assoc.moveTo(0);
    updateAssociation(assoc, subClass, superClass, 0, 0, []);
    canvas.renderAll();
    assocPairs.push([assoc, subClass, superClass]);
    showClassEditor();
}

function deleteInheritanceFromCanvas(inherit) {
    var objects = canvas.getObjects();
    for (var y = 0; y < objects.length; y++) {
        if (objects[y].id == JSON.stringify(inherit)) {
            assocPairs = assocPairs.filter(e => e[0].id != objects[y].id);
            canvas.remove(objects[y]);
        }
    }
    showClassEditor();
}

function deleteAssociationFromCanvas(ass) {
    var objects = canvas.getObjects();
    for (var y = 0; y < objects.length; y++) {
        if (objects[y].id == JSON.stringify(ass)) {
            assocPairs = assocPairs.filter(e => e[0].id != objects[y].id);
            canvas.remove(objects[y]);
        }
    }
    showClassEditor();
}

function resetAttributesOnCanvas() {
       // empty the group object and redraw the group elements
       var group = canvas.getActiveObject();
       if (group.item(2).text != "") {
           group.item(2).text += "\n";
       }
       var attText = "";
       var attributes = schemaDefinedDataProperties.filter(e => e['domain'] == group.id);
       for (var x = 0; x < attributes.length; x++) {
            if (x != 0)
                attText += "\n";
            var split = attributes[x]["property"].split("#");
            attText += "+" + split[split.length-1].split(">")[0] + " : " + attributes[x]['range'] + " [" + attributes[x]['minCardinality'] + ".." + attributes[x]['maxCardinality'] + "]";
       }

       var tempWidth = group.width;
       group.item(2).text = attText;
       group.dirty = true;
       canvas.renderAll();

       group.addWithUpdate();
       if (tempWidth != group.width) {
           group.item(0).width = group.width + 6;
           group.item(1).width = group.width + 6;
       }
       // every att increases the height
       if (attributes.length > 0)
            group.item(0).height = group.height - 22;
       group.addWithUpdate();
       group.dirty = true;
       canvas.renderAll();
       showClassEditor();
       updateAssociations();
}

function closePopup() {
    document.getElementById("Popup").style.display = "None";
    document.getElementById("PopupOverlay").style.display = "None";
}

function showClassEditor() {
    if (canvas.getActiveObject().isType('group')) {
        canvas.getActiveObject().set({'borderColor':'#e56c00'});
        classEditor.style.right = "0px";
        document.getElementById("ClassName").innerHTML = canvas.getActiveObject().item(3).text;

        // load properties
        var activeProperties = document.getElementById("activeProperties");
        activeProperties.innerHTML = "";
        var dataProperties = schemaDefinedDataProperties.filter(e => e['domain'] == canvas.getActiveObject().id);
        for (var x = 0; x < dataProperties.length; x++) {
            activeProperties.innerHTML += "<div onclick=deleteAttribute(" + escapeHtml(JSON.stringify(dataProperties[x])) + ")>" + escapeHtml(dataProperties[x]['property']) + " : " + escapeHtml(dataProperties[x]['range']) + " <b>Verwijder</b></div>";
        }
        var activeAssociations = document.getElementById("activeAssociations");
        activeAssociations.innerHTML = "";
        var associations = schemaDefinedObjectProperties.filter(e => e['domain'] == canvas.getActiveObject().id);
        for (var y = 0; y < associations.length; y++) {
            activeAssociations.innerHTML += "<div onclick=deleteAssociation(" + escapeHtml(JSON.stringify(associations[y])) + ")>" + escapeHtml(associations[y]['domain']) + " : " + escapeHtml(associations[y]['range']) + " <b>Verwijder</b></div>";
        }
        var activeInheritance = document.getElementById("activeInheritance");
        activeInheritance.innerHTML = "";
        var inheritance = schemaDefinedInheritance.filter(e => e['subClass'] == canvas.getActiveObject().id);
        for (var z = 0; z < inheritance.length; z++) {
            activeInheritance.innerHTML += "<div onclick=deleteInheritance(" + escapeHtml(JSON.stringify(inheritance[z])) + ")>" + escapeHtml(inheritance[z]['subClass']) + " -> " + escapeHtml(inheritance[z]['superClass']) + " <b>Verwijder</b></div>";
        }
    }
}

function escapeHtml(unsafe) {
    return unsafe
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
 }

function selectObject(id) {
    var objects = canvas.getObjects();
    for (var x = 0; x < objects.length; x++ ) {
        if (objects[x].id === id) {
            return objects[x];
        }
    }
}

function updateAssociations() {
    // check if class is moving our selection of classes
    var active = canvas.getActiveObject();
    var top = 0;
    var left = 0;
    var grouped = [];

    if (active != undefined && active.id === undefined) {
        top = active.top + (active.height / 2);
        left = active.left + (active.width / 2);
        var objects = active.getObjects();
        for (var x = 0; x < objects.length; x++) {
            grouped.push(objects[x]);
        }
    }

    for (var x = 0; x < assocPairs.length; x++) {
        updateAssociation(assocPairs[x][0], assocPairs[x][1], assocPairs[x][2], top, left, grouped);
        assocPairs[x][0].moveTo(0);
    }
}

function setActiveCanvasObject(IRI) {
    var objects = canvas.getObjects();
    for (var x = 0; x < objects.length; x++) {
        if (objects[x].id == IRI) {
            canvas.setActiveObject(objects[x]);
        }
    }
}

function handleResize() {
    var canvasContainer = document.getElementById("Right");
    canvas.setWidth(canvasContainer.offsetWidth);
    canvas.setHeight(canvasContainer.offsetHeight);
}

window.onresize = handleResize;

document.addEventListener('DOMContentLoaded', function(){
    // drawing the editor
    // https://www.html5rocks.com/en/tutorials/canvas/hidpi/
    canvas = new fabric.Canvas('Canvas', {fireRightClick: true});
    var container = document.getElementById("Right");
    classEditor = document.getElementById("ClassEditor");
    canvas.setHeight(container.offsetHeight);
    canvas.setWidth(container.offsetWidth);

    canvas.on('selection:created', function() {
        showClassEditor();
    });
    canvas.on('selection:updated', function() {
        showClassEditor();
    });
    canvas.on('selection:cleared', function() {
        classEditor.style.right = "-350px";
    });

    var timeout;

    canvas.on('mouse:wheel', function(opt) {
        var delta = opt.e.deltaY;
        var zoom = canvas.getZoom();
        var dir = delta/2000 * -1;
        zoom = zoom + dir;
        if (zoom > 20) zoom = 20;
        if (zoom < 0.01) zoom = 0.01;
        canvas.setZoom(zoom);
        opt.e.preventDefault();
        opt.e.stopPropagation();
        var zoomElement = document.getElementById("ZoomIn");
        if (dir < 0) zoomElement = document.getElementById("ZoomOut");
        zoomElement.classList.add("toolHover");
        clearTimeout(timeout);
        timeout = setTimeout(function() {
          zoomElement.classList.remove("toolHover");
        }, 100);
    });

    canvas.on('mouse:down', function(opt) {
        var evt = opt.e;
        if (opt.button === 3) {
            this.isDragging = true;
            this.selection = false;
            this.lastPosX = evt.clientX;
            this.lastPosY = evt.clientY;
        }
    });
    canvas.on('mouse:move', function(opt) {
        if (this.isDragging) {
            var e = opt.e;
            //this.viewportTransform[4] += e.clientX - this.lastPosX;
            //this.viewportTransform[5] += e.clientY - this.lastPosY;
            var objects = this.getObjects();
            for(var x = 0; x < objects.length; x++) {
                if (objects[x].elementType != undefined) {
                    objects[x].top += e.clientY - this.lastPosY;
                    objects[x].left += e.clientX - this.lastPosX;
                    objects[x].setCoords();
                }
            }
            updateAssociations();
            this.requestRenderAll();
            this.lastPosX = e.clientX;
            this.lastPosY = e.clientY;
        }
    });
    canvas.on('mouse:up', function(opt) {
        this.isDragging = false;
        this.selection = true;
    });

    canvas.on('object:moving', function(e) {
        updateAssociations();
        canvas.renderAll();
    });

    document.addEventListener('contextmenu', event => event.preventDefault());
 }, false);