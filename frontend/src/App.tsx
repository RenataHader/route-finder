import React, { useState, useCallback } from 'react';
import MapView from './components/MapView';
import ResultsList from './components/ResultsList';
import type { GeoPoint, RouteLockerResult, SelectionStep } from './types/types';
import { fetchRouteLockers } from './services/api';
import './App.css';

/**
 * Główny komponent aplikacji InPost Route Finder.
 *
 * Przepływ:
 * 1. Użytkownik klika na mapie → ustawia punkt startowy (A)
 * 2. Kolejny klik → ustawia punkt docelowy (B)
 * 3. Aplikacja pobiera z backendu TOP 5 paczkomatów leżących po drodze
 * 4. Wyniki wyświetlane są na mapie i w liście bocznej
 */
const App: React.FC = () => {
  // ── Stan aplikacji ────────────────────────────────────────────────────────
  const [start, setStart] = useState<GeoPoint | null>(null);
  const [end, setEnd] = useState<GeoPoint | null>(null);
  const [step, setStep] = useState<SelectionStep>('selectStart');
  const [results, setResults] = useState<RouteLockerResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [maxDistanceKm, setMaxDistanceKm] = useState(1.0);
  const [routeMessage, setRouteMessage] = useState<string | null>(null);
  const [expandedSearchUsed, setExpandedSearchUsed] = useState(false);

  // Obsługa kliknięcia na mapie
  const handleMapClick = useCallback((point: GeoPoint) => {
    if (step === 'selectStart') {
      setStart(point);
      setStep('selectEnd');
      setResults([]);
      setError(null);
    } else if (step === 'selectEnd') {
      setEnd(point);
      setStep('done');
    }
  }, [step]);

  // Wyszukiwanie paczkomatów
  const handleSearch = useCallback(async () => {
    if (!start || !end) return;

    setLoading(true);
    setError(null);
    setResults([]);

    try {
      const data = await fetchRouteLockers(start, end, maxDistanceKm);

      setResults(data.lockers);
      setRouteMessage(data.message);
      setExpandedSearchUsed(data.expandedSearchUsed);

      if (data.lockers.length === 0) {
        setError(null);
      }
    } catch (err: any) {
      setError(err?.response?.data?.error || 'Błąd połączenia z backendem. Czy backend jest uruchomiony?');
    } finally {
      setLoading(false);
    }
  }, [start, end, maxDistanceKm]);

  //  Reset do stanu początkowego
  const handleReset = useCallback(() => {
    setStart(null);
    setEnd(null);
    setStep('selectStart');
    setResults([]);
    setError(null);
    setRouteMessage(null);
    setExpandedSearchUsed(false);
  }, []);

  // Etykieta instrukcji
  const getStepLabel = () => {
    switch (step) {
      case 'selectStart': return '👆 Kliknij na mapie, aby wybrać punkt startowy (A)';
      case 'selectEnd':   return '👆 Kliknij na mapie, aby wybrać punkt docelowy (B)';
      case 'done':        return '✅ Oba punkty wybrane — kliknij "Szukaj"';
    }
  };

  return (
    <div className="app">
      {/* Nagłówek */}
      <header className="app-header">
        <div className="header-title">
          <span className="header-icon">📦</span>
          <h1>InPost Route Finder</h1>
          <span className="header-subtitle">Znajdź paczkomat po drodze</span>
        </div>
      </header>

      {/* Główny layout */}
      <div className="app-body">
        {/* ── Panel boczny  */}
        <aside className="sidebar">
          {/* Instrukcja */}
          <div className="step-indicator">
            {getStepLabel()}
          </div>

          {/* Ustawienia */}
          <div className="settings-section">
            <label>
              Max odległość od trasy:
              <div className="slider-row">
                <input
                  type="range"
                  min="0.5"
                  max="10"
                  step="0.5"
                  value={maxDistanceKm}
                  onChange={e => setMaxDistanceKm(Number(e.target.value))}
                />
                <span className="slider-value">{maxDistanceKm} km</span>
              </div>
            </label>
          </div>

          {/* Informacje o wybranych punktach */}
          <div className="points-info">
            <div className={`point-badge ${start ? 'point-badge--set' : ''}`}>
              🟢 Start: {start ? `${start.lat.toFixed(4)}, ${start.lng.toFixed(4)}` : 'nie wybrano'}
            </div>
            <div className={`point-badge ${end ? 'point-badge--set' : ''}`}>
              🔴 Cel: {end ? `${end.lat.toFixed(4)}, ${end.lng.toFixed(4)}` : 'nie wybrano'}
            </div>
          </div>

          {/* Przyciski akcji */}
          <div className="action-buttons">
            <button
              className="btn btn-primary"
              onClick={handleSearch}
              disabled={!start || !end || loading}
            >
              {loading ? '⏳ Szukam...' : '🔍 Szukaj paczkomatów'}
            </button>
            <button className="btn btn-secondary" onClick={handleReset}>
              🔄 Reset
            </button>
          </div>

          {/* Lista wyników */}
          <ResultsList
            results={results}
            loading={loading}
            error={error}
            message={routeMessage}
            expandedSearchUsed={expandedSearchUsed}
          />
        </aside>

        {/* Mapa*/}
        <main className="map-wrapper">
          <MapView
            start={start}
            end={end}
            results={results}
            step={step}
            onMapClick={handleMapClick}
          />
        </main>
      </div>
    </div>
  );
};

export default App;
