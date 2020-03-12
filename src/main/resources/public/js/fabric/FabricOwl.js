// https://stackoverflow.com/questions/42095045/fabric-js-itext-font-fuzzy-according-to-position
fabric.Object.prototype.objectCaching = false;

var fontName = "Poppins";

// TODO fix bug where height of classes is not updated properly when not rendered in canvas

function getFabricOwlClass(classData) {
    var classType = "Class";
    var classFill = "#fffcc9";
    //var classFill = "#ffffff";
    var strokeColor = "#bfbfbf";
    strokeColor = "#969696";
    if (classData["type"] != null) {
        classType = classData["type"];
        if (classType === "Enum")
            classFill = "#c2ffa3";
    }

    // set class name
    var name = new fabric.Text(classData["name"], {
        fontSize: 14,
        fontWeight: 'bold',
        fontFamily: fontName
    });

    var attText = "";

    for (x = 0; x < classData["attributes"].length; x++) {
        var att = Object.keys(classData["attributes"][x])[0];
        if (x != 0) {
            attText += "\n";
        }
        attText += att + " : " + classData["attributes"][x][att];
    }

    var attributes = new fabric.Text(attText, {
        top: 28,
        fontSize: 12,
        fontFamily: fontName,
        lineHeight : 1.5
    });

    // group together as class object - introduce type [class, enum]
    var owlClass = new fabric.Group([name, attributes], { id : classData["IRI"], elementType : classType });

    // TODO Refactor the padding out of the group
    var padding = 12;

    // add a box around the class
    // add rect around group
    var border = new fabric.Rect({
        // position from group center
        left: -0.5*owlClass.width-(padding/2),
        top: -0.5*owlClass.height-(padding/2),
        width: owlClass.width+padding,
        height: owlClass.height+padding,
        stroke: strokeColor,
        strokeWidth: 1,
        fill: classFill
    });
    var header = new fabric.Line([-0.5*owlClass.width-(padding/2), -0.5*owlClass.height-(padding/2)+28, 0.5*owlClass.width+(padding/2), -0.5*owlClass.height-(padding/2)+28], {
        fill: '#696969',
        stroke: strokeColor,
        strokeWidth: 1,
        selectable: false,
     });

    var shadow = {
        color: 'rgb(0,0,0,0.17)',
        blur: 18,
        offsetX: 0,
        offsetY: 0,
    }
    border.setShadow(shadow);

    owlClass.add(border);
    owlClass.add(header);
    border.moveTo(0);
    header.moveTo(1);
    attributes.moveTo(2);
    name.moveTo(3);
    owlClass.addWithUpdate();
    owlClass.hasControls = false;
    owlClass.dirty = true;
    return owlClass;
}

// drawing associations between different classes
function getAssociation(startObj, endObj, label, assocID) {
    var labelText = label;
    var fillColor = 'black';
    if (label === "inherits") {
        fillColor = 'white';
        labelText = '';
    }

    var line = new fabric.Line([1, 1, 0, 0], {
        fill: '#696969',
        stroke: '#696969',
        strokeWidth: 1
    });

    var assoc = new fabric.Text(labelText, {
        fontSize: 11,
        fontFamily: fontName,
        lineHeight : 1.5,
        originX: 'left',
        originY: 'top'
    });

    // triangle
    var triangle = new fabric.Triangle({
        fill: fillColor,
        stroke: '#696969',
        strokeWidth: 1,
        height: 12,
        width: 12,
        originX: 'center'
    });

    var association = new fabric.Group([line, assoc, triangle], { id : assocID});
    association.set({
        originX: "center",
        originY: "center"
    });
    association.selectable = false;
    return association;
}

// https://jsfiddle.net/ka7nhvbq/2/
// update associations on drag of the objects
function updateAssociation(assoc, startObj, endObj, top, left, grouped) {
    // if there is an offset
    var pos1 = startObj.getCenterPoint();
    var pos2 = endObj.getCenterPoint();

    if (grouped.indexOf(startObj) != -1) {
        pos1.x += left;
        pos1.y += top;
    }

    if (grouped.indexOf(endObj) != -1) {
        pos2.x += left;
        pos2.y += top;
    }

    // calculate the connection sides
    var intersection =  getIntersectionPoint(pos1, pos2, endObj.width / 2, endObj.height / 2);

    if (intersection) {
        assoc.item(1).originX = 'right';
        if (intersection.direction == 2)
            assoc.item(1).originX = 'left';
        pos2.x = intersection.x;
        pos2.y = intersection.y;
        assoc.item(0).set({ 'x1': pos1.x, 'y1': pos1.y, 'x2': pos2.x, 'y2': pos2.y });

        var assocPoint = pos1.lerp(pos2, 0.8);
        assoc.item(1).top = assocPoint.y;
        assoc.item(1).left = assocPoint.x;
        assoc.item(2).angle = getAngle(pos1, pos2);
        assoc.item(2).top = pos2.y;
        assoc.item(2).left = pos2.x;
    }
    assoc.dirty = true;
}

function getIntersectionPoint(pos1, pos2, width, height) {
    // check each of the four sides making up the endObj
    var bLeft = { 'x': pos2.x - width + 6, 'y': pos2.y + height};
    var bRight = { 'x': pos2.x + width + 6, 'y': pos2.y + height};
    var tLeft = { 'x': pos2.x - width + 6, 'y': pos2.y - height};
    var tRight = {'x': pos2.x + width + 6, 'y': pos2.y - height};
    // checking from left, top, right, bottom
    var left = intersect(pos1.x, pos1.y, pos2.x, pos2.y, bLeft.x, bLeft.y, tLeft.x, tLeft.y);
    if (left) {
        left.direction = 0;
        return left;
    }
    var top = intersect(pos1.x, pos1.y, pos2.x, pos2.y, tLeft.x, tLeft.y, tRight.x, tRight.y);
    if (top) {
        top.direction = 1;
        return top;
    }
    var right = intersect(pos1.x, pos1.y, pos2.x, pos2.y, tRight.x, tRight.y, bRight.x, bRight.y);
    if (right) {
        right.direction = 2;
        return right;
    }
    var bottom = intersect(pos1.x, pos1.y, pos2.x, pos2.y, bLeft.x, bLeft.y, bRight.x, bRight.y);
    if (bottom) {
        bottom.direction = 3;
        return bottom;
    }
}

// line intercept math by Paul Bourke http://paulbourke.net/geometry/pointlineplane/
// Determine the intersection point of two line segments
// Return FALSE if the lines don't intersect
function intersect(x1, y1, x2, y2, x3, y3, x4, y4) {
  // Check if none of the lines are of length 0
	if ((x1 === x2 && y1 === y2) || (x3 === x4 && y3 === y4)) {
		return false
	}
	denominator = ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1))
  // Lines are parallel
	if (denominator === 0) {
		return false
	}
	let ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator
	let ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator
  // is the intersection along the segments
	if (ua < 0 || ua > 1 || ub < 0 || ub > 1) {
		return false
	}
  // Return a object with the x and y coordinates of the intersection
	let x = x1 + ua * (x2 - x1)
	let y = y1 + ua * (y2 - y1)
	return {x, y}
}

function getAngle(pos1, pos2) {
    dx = pos2.x - pos1.x;
    dy = pos2.y - pos1.y;
    angle = Math.atan2(dy, dx);
    angle *= 180 / Math.PI;
    angle += 90;
    return angle;
}