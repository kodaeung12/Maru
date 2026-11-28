"use strict";

const [latitude, longitude, mode] = location.hash.slice(1).split(",");
const point = [Number(latitude), Number(longitude)];
const interactive = mode === "true";

const map = L.map("map", {
    zoomControl: false,
    minZoom: 3,
    maxZoom: 19,
    dragging: interactive,
    touchZoom: interactive,
    scrollWheelZoom: interactive,
    doubleClickZoom: interactive,
    boxZoom: interactive,
    keyboard: interactive
}).setView(point, 16);

map.attributionControl.setPrefix(false);
L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    maxZoom: 19,
    keepBuffer: 1
}).on("tileload", () => { window.maruMapState = 1; }).addTo(map);

L.marker(point, {
    icon: L.icon({ iconUrl: "marker.png", iconSize: [31, 41], iconAnchor: [15.5, 40] }),
    interactive: false,
    keyboard: false
}).addTo(map);

window.maruZoom = step => {
    if (interactive) {
        map.setZoom(map.getZoom() + step, { animate: true });
    }
};
