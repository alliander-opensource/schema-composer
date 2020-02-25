function exportToSVG() {
    var a = document.createElement('a');
    var svg = canvas.toSVG().replace(/id="{.*?}"/g, "");
    a.href = 'data:image/svg+xml,'+encodeURIComponent(svg);
    a.download = 'schema.svg';
    a.click();
}