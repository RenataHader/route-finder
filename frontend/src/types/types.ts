export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface Locker {
  id: string;
  name: string;
  address: string;
  city: string;
  lat: number;
  lng: number;
}

export interface RouteLockerResult {
  locker: Locker;
  distanceFromRouteKm: number;
  projectionFactor: number;
  outsideRequestedRange: boolean;
  requestedCorridorKm: number;
}

export interface RouteLockersResponse {
  lockers: RouteLockerResult[];
  expandedSearchUsed: boolean;
  requestedCorridorKm: number;
  usedCorridorKm: number;
  message: string;
}

export type SelectionStep = 'selectStart' | 'selectEnd' | 'done';