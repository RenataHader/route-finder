import React from 'react';
import {
  MapContainer,
  TileLayer,
  Marker,
  Polyline,
  Popup,
  CircleMarker,
  useMapEvents,
} from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { GeoPoint, RouteLockerResult, SelectionStep } from '../types/types';

delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// Niestandardowa ikona dla punktu startowego (zielona)
const startIcon = L.divIcon({
  html: `<div style="
    width:30px;height:30px;border-radius:50%;
    background:#22c55e;border:3px solid white;
    box-shadow:0 2px 8px rgba(0,0,0,0.4);
    display:flex;align-items:center;justify-content:center;
    color:white;font-weight:bold;font-size:14px;
  ">A</div>`,
  iconSize: [30, 30],
  iconAnchor: [15, 15],
  className: '',
});

// ── Niestandardowa ikona dla punktu docelowego (czerwona)
const endIcon = L.divIcon({
  html: `<div style="
    width:30px;height:30px;border-radius:50%;
    background:#ef4444;border:3px solid white;
    box-shadow:0 2px 8px rgba(0,0,0,0.4);
    display:flex;align-items:center;justify-content:center;
    color:white;font-weight:bold;font-size:14px;
  ">B</div>`,
  iconSize: [30, 30],
  iconAnchor: [15, 15],
  className: '',
});

// Props

interface MapViewProps {
  start: GeoPoint | null;
  end: GeoPoint | null;
  results: RouteLockerResult[];
  step: SelectionStep;
  onMapClick: (point: GeoPoint) => void;
}

// Komponent obsługujący kliknięcia na mapie

function ClickHandler({ onMapClick }: { onMapClick: (p: GeoPoint) => void }) {
  useMapEvents({
    click(e) {
      onMapClick({ lat: e.latlng.lat, lng: e.latlng.lng });
    },
  });
  return null;
}

const MapView: React.FC<MapViewProps> = ({ start, end, results, step, onMapClick }) => {
  // Domyślne centrum mapy — Polska
  const center: [number, number] = [52.0, 19.5];
  const mapStyle: React.CSSProperties = {
    height: '100%',
    width: '100%',
    cursor: step !== 'done' ? 'crosshair' : 'grab',
  };

  return (
    <MapContainer center={center} zoom={6} style={mapStyle}>
      {/* Warstwa kafli OpenStreetMap */}
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* Obsługa kliknięć — tylko gdy czekamy na wybór punktu */}
      {step !== 'done' && <ClickHandler onMapClick={onMapClick} />}

      {/* Marker punktu startowego */}
      {start && (
        <Marker position={[start.lat, start.lng]} icon={startIcon}>
          <Popup>
            <strong>Start</strong><br />
            {start.lat.toFixed(5)}, {start.lng.toFixed(5)}
          </Popup>
        </Marker>
      )}

      {/* Marker punktu docelowego */}
      {end && (
        <Marker position={[end.lat, end.lng]} icon={endIcon}>
          <Popup>
            <strong>Cel</strong><br />
            {end.lat.toFixed(5)}, {end.lng.toFixed(5)}
          </Popup>
        </Marker>
      )}

      {/* Linia trasy między start a end */}
      {start && end && (
        <Polyline
          positions={[
            [start.lat, start.lng],
            [end.lat, end.lng],
          ]}
          color="#3b82f6"
          weight={3}
          opacity={0.7}
          dashArray="8 4"
        />
      )}

      {/* Paczkomaty wynikowe */}
      {results.map((result, index) => {
        const isBest = index === 0;

        return (
          <CircleMarker
            key={result.locker.id}
            center={[result.locker.lat, result.locker.lng]}
            radius={isBest ? 12 : 8}
            fillColor={isBest ? '#f59e0b' : '#6366f1'}  // złoty = najlepszy, fioletowy = pozostałe
            color={isBest ? '#d97706' : '#4338ca'}
            weight={2}
            opacity={1}
            fillOpacity={0.85}
          >
            <Popup>
              <div style={{ minWidth: '200px' }}>
                <strong style={{ fontSize: '14px' }}>
                  {isBest ? '⭐ ' : `#${index + 1} `}
                  {result.locker.name}
                </strong>
                {result.outsideRequestedRange && (
                  <div style={{ color: '#b45309', fontWeight: 600, marginTop: '4px' }}>
                    ⚠️ Poza wybranym zakresem
                  </div>
                )}
                <hr style={{ margin: '6px 0' }} />
                <div>{result.locker.address}</div>
                <div style={{ color: '#666' }}>{result.locker.city}</div>
                <hr style={{ margin: '6px 0' }} />
                <div>Od trasy: <strong>{result.distanceFromRouteKm.toFixed(3)} km</strong></div>
              </div>
            </Popup>
          </CircleMarker>
        );
      })}
    </MapContainer>
  );
};

export default MapView;
