import axios from 'axios';
import type { GeoPoint, RouteLockersResponse } from '../types/types';

const BASE_URL = '/api';

export async function fetchRouteLockers(
  start: GeoPoint,
  end: GeoPoint,
  maxDistanceKm: number = 1.0
): Promise<RouteLockersResponse> {
  const params = new URLSearchParams({
    startLat: String(start.lat),
    startLng: String(start.lng),
    endLat: String(end.lat),
    endLng: String(end.lng),
    maxDistanceKm: String(maxDistanceKm),
  });

  const response = await axios.get<RouteLockersResponse>(
    `${BASE_URL}/route-lockers?${params}`
  );

  return response.data;
}