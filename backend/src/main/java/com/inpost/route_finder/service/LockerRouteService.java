package com.inpost.route_finder.service;

import com.inpost.route_finder.client.InPostClient;
import com.inpost.route_finder.model.GeoPoint;
import com.inpost.route_finder.model.Locker;
import com.inpost.route_finder.model.RouteLockerResult;
import com.inpost.route_finder.model.RouteLockersResponse;
import com.inpost.route_finder.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LockerRouteService {

    private static final Logger log = LoggerFactory.getLogger(LockerRouteService.class);

    private static final int TOP_N = 5;
    private static final double MAX_FALLBACK_CORRIDOR_KM = 50.0;
    private static final double MIN_API_SEARCH_RADIUS_KM = 10.0;
    private static final double MAX_API_SEARCH_RADIUS_KM = 50.0;

    private final InPostClient inPostClient;

    public LockerRouteService(InPostClient inPostClient) {
        this.inPostClient = inPostClient;
    }

    public RouteLockersResponse findLockersAlongRoute(
            GeoPoint start,
            GeoPoint end,
            double requestedCorridorKm
    ) {
        double safeRequestedCorridorKm = normalizeRequestedCorridor(requestedCorridorKm);

        log.info(
                "Szukam paczkomatów: start={},{} end={},{} wybrany promień={} km",
                start.getLat(),
                start.getLng(),
                end.getLat(),
                end.getLng(),
                safeRequestedCorridorKm
        );

        List<RouteLockerResult> exactResults = findLockersInCorridor(
                start,
                end,
                safeRequestedCorridorKm,
                safeRequestedCorridorKm
        );

        if (!exactResults.isEmpty()) {
            String message = "Znaleziono paczkomaty w wybranym promieniu "
                    + formatKm(safeRequestedCorridorKm)
                    + " km od trasy.";

            return new RouteLockersResponse(
                    exactResults,
                    false,
                    safeRequestedCorridorKm,
                    safeRequestedCorridorKm,
                    message
            );
        }

        List<Double> fallbackCorridors = buildFallbackCorridors(safeRequestedCorridorKm);

        for (double fallbackCorridorKm : fallbackCorridors) {
            List<RouteLockerResult> fallbackResults = findLockersInCorridor(
                    start,
                    end,
                    fallbackCorridorKm,
                    safeRequestedCorridorKm
            );

            if (!fallbackResults.isEmpty()) {
                List<RouteLockerResult> actuallyInsideRequestedRange = fallbackResults.stream()
                        .filter(result -> result.getDistanceFromRouteKm() <= safeRequestedCorridorKm)
                        .limit(TOP_N)
                        .toList();

                if (!actuallyInsideRequestedRange.isEmpty()) {
                    String message = "Znaleziono paczkomaty w wybranym promieniu "
                            + formatKm(safeRequestedCorridorKm)
                            + " km od trasy.";

                    return new RouteLockersResponse(
                            actuallyInsideRequestedRange,
                            false,
                            safeRequestedCorridorKm,
                            safeRequestedCorridorKm,
                            message
                    );
                }

                String message = "Nie znaleziono paczkomatów w wybranym promieniu "
                        + formatKm(safeRequestedCorridorKm)
                        + " km od trasy. Pokazujemy najbliższe znalezione w promieniu "
                        + formatKm(fallbackCorridorKm)
                        + " km.";

                return new RouteLockersResponse(
                        fallbackResults,
                        true,
                        safeRequestedCorridorKm,
                        fallbackCorridorKm,
                        message
                );
            }
        }

        String message = "Nie znaleziono paczkomatów w pobliżu wybranej trasy.";

        return new RouteLockersResponse(
                List.of(),
                false,
                safeRequestedCorridorKm,
                safeRequestedCorridorKm,
                message
        );
    }

    private List<RouteLockerResult> findLockersInCorridor(
            GeoPoint start,
            GeoPoint end,
            double corridorKm,
            double requestedCorridorKm
    ) {
        List<Locker> candidates = fetchCandidateLockersNearRoute(start, end, corridorKm);

        log.info(
                "Pobrano {} kandydatów dla promienia {} km. Rozpoczynam dokładne filtrowanie.",
                candidates.size(),
                corridorKm
        );

        return candidates.stream()
                .map(locker -> evaluateLocker(locker, start, end, requestedCorridorKm))
                .filter(result -> result.getProjectionFactor() >= 0.0 && result.getProjectionFactor() <= 1.0)
                .filter(result -> result.getDistanceFromRouteKm() <= corridorKm)
                .sorted(Comparator.comparingDouble(RouteLockerResult::getDistanceFromRouteKm))
                .limit(TOP_N)
                .toList();
    }

    private List<Locker> fetchCandidateLockersNearRoute(
            GeoPoint start,
            GeoPoint end,
            double corridorKm
    ) {
        double routeLengthKm = GeoUtils.haversineDistanceKm(
                start.getLat(),
                start.getLng(),
                end.getLat(),
                end.getLng()
        );

        double apiSearchRadiusKm = Math.min(
                MAX_API_SEARCH_RADIUS_KM,
                Math.max(MIN_API_SEARCH_RADIUS_KM, corridorKm)
        );

        int apiSearchRadiusMeters = (int) Math.round(apiSearchRadiusKm * 1000.0);
        int samples = calculateSampleCount(routeLengthKm, apiSearchRadiusKm);

        Map<String, Locker> uniqueLockers = new LinkedHashMap<>();

        for (int i = 0; i <= samples; i++) {
            double t = samples == 0 ? 0.0 : (double) i / samples;
            GeoPoint samplePoint = interpolate(start, end, t);

            List<Locker> nearby = inPostClient.fetchNearbyLockers(samplePoint, apiSearchRadiusMeters);

            for (Locker locker : nearby) {
                uniqueLockers.put(locker.getId(), locker);
            }
        }

        return new ArrayList<>(uniqueLockers.values());
    }

    private RouteLockerResult evaluateLocker(
            Locker locker,
            GeoPoint start,
            GeoPoint end,
            double requestedCorridorKm
    ) {
        GeoPoint lockerPoint = new GeoPoint(locker.getLat(), locker.getLng());

        double distanceFromRouteKm = GeoUtils.distancePointToSegmentKm(lockerPoint, start, end);
        double projectionFactor = GeoUtils.projectionFactor(lockerPoint, start, end);

        boolean outsideRequestedRange = distanceFromRouteKm > requestedCorridorKm;

        return new RouteLockerResult(
                locker,
                distanceFromRouteKm,
                projectionFactor,
                outsideRequestedRange,
                requestedCorridorKm
        );
    }

    private int calculateSampleCount(double routeLengthKm, double apiSearchRadiusKm) {
        if (routeLengthKm <= 0.1) {
            return 1;
        }

        double stepKm = Math.max(8.0, apiSearchRadiusKm * 1.2);
        int samples = (int) Math.ceil(routeLengthKm / stepKm);

        return Math.max(1, Math.min(samples, 80));
    }

    private GeoPoint interpolate(GeoPoint start, GeoPoint end, double t) {
        double lat = start.getLat() + (end.getLat() - start.getLat()) * t;
        double lng = start.getLng() + (end.getLng() - start.getLng()) * t;

        return new GeoPoint(lat, lng);
    }

    private double normalizeRequestedCorridor(double requestedCorridorKm) {
        if (Double.isNaN(requestedCorridorKm) || requestedCorridorKm <= 0.0) {
            return 1.0;
        }

        return Math.min(requestedCorridorKm, MAX_FALLBACK_CORRIDOR_KM);
    }

    private List<Double> buildFallbackCorridors(double requestedCorridorKm) {
        List<Double> fallback = new ArrayList<>();

        addFallback(fallback, requestedCorridorKm * 2.0, requestedCorridorKm);
        addFallback(fallback, requestedCorridorKm * 4.0, requestedCorridorKm);
        addFallback(fallback, 10.0, requestedCorridorKm);
        addFallback(fallback, 20.0, requestedCorridorKm);
        addFallback(fallback, 50.0, requestedCorridorKm);

        fallback.sort(Double::compareTo);

        return fallback;
    }

    private void addFallback(List<Double> fallback, double value, double requestedCorridorKm) {
        double rounded = Math.min(MAX_FALLBACK_CORRIDOR_KM, Math.ceil(value * 10.0) / 10.0);

        if (rounded <= requestedCorridorKm) {
            return;
        }

        boolean alreadyExists = fallback.stream()
                .anyMatch(existing -> Math.abs(existing - rounded) < 0.001);

        if (!alreadyExists) {
            fallback.add(rounded);
        }
    }

    private String formatKm(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001) {
            return String.valueOf((int) Math.round(value));
        }

        return String.format("%.1f", value);
    }
}