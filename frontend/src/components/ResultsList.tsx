import React from 'react';
import type { RouteLockerResult } from '../types/types';

interface ResultsListProps {
  results: RouteLockerResult[];
  loading: boolean;
  error: string | null;
  message?: string | null;
  expandedSearchUsed?: boolean;
}

const ResultsList: React.FC<ResultsListProps> = ({
  results,
  loading,
  error,
  message,
  expandedSearchUsed = false,
}) => {
  if (loading) {
    return (
      <div className="results-panel">
        <h3>Szukam paczkomatów...</h3>
        <div className="loading-spinner">⏳</div>
        <p style={{ color: '#666', fontSize: '13px' }}>
          Szukam paczkomatów w wybranym regionie trasy.
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="results-panel">
        <h3>❌ Błąd</h3>
        <p style={{ color: '#ef4444' }}>{error}</p>
      </div>
    );
  }

  if (results.length === 0) {
    return (
      <div className="results-panel">
        <h3>Brak wyników</h3>
        <p style={{ color: '#666' }}>
          {message || 'Nie znaleziono paczkomatów w pobliżu wybranej trasy.'}
        </p>
      </div>
    );
  }

  return (
    <div className="results-panel">
      <h3>📦 TOP {results.length} paczkomatów po drodze</h3>

      {message && (
        <div
          style={{
            marginBottom: '10px',
            padding: '8px 10px',
            borderRadius: '8px',
            fontSize: '12px',
            background: expandedSearchUsed ? '#fef3c7' : '#ecfdf5',
            color: expandedSearchUsed ? '#92400e' : '#166534',
            border: expandedSearchUsed ? '1px solid #fcd34d' : '1px solid #bbf7d0',
          }}
        >
          {expandedSearchUsed ? '⚠️ ' : '✅ '}
          {message}
        </div>
      )}

      <div className="results-list">
        {results.map((result, index) => {
          const isBest = index === 0;

          return (
            <div
              key={result.locker.id}
              className={`result-item ${isBest ? 'result-item--best' : ''}`}
            >
              <div className="result-rank">
                {isBest ? '⭐' : `#${index + 1}`}
              </div>

              <div className="result-info">
                <div className="result-name">
                  {result.locker.name}
                  {result.outsideRequestedRange && (
                    <span
                      style={{
                        marginLeft: '6px',
                        fontSize: '11px',
                        color: '#b45309',
                        fontWeight: 600,
                      }}
                    >
                      poza wybranym zakresem
                    </span>
                  )}
                </div>

                <div className="result-address">
                  {result.locker.address}
                  {result.locker.city ? `, ${result.locker.city}` : ''}
                </div>
              </div>

              <div className="result-metrics">
                <div className="metric">
                  <span className="metric-label">od trasy</span>
                  <span className="metric-value">
                    {result.distanceFromRouteKm < 1
                      ? `${(result.distanceFromRouteKm * 1000).toFixed(0)} m`
                      : `${result.distanceFromRouteKm.toFixed(1)} km`}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ResultsList;