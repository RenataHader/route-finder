package com.inpost.route_finder.controller;

import com.inpost.route_finder.model.GeoPoint;
import com.inpost.route_finder.model.RouteLockersResponse;
import com.inpost.route_finder.service.LockerRouteService;
import com.inpost.route_finder.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class LockerController {

    private static final Logger log = LoggerFactory.getLogger(LockerController.class);

    private static final double MAX_ROUTE_LENGTH_KM = 300.0;

    private final LockerRouteService lockerRouteService;

    public LockerController(LockerRouteService lockerRouteService) {
        this.lockerRouteService = lockerRouteService;
    }

    @GetMapping("/route-lockers")
    public ResponseEntity<?> getRouteLockers(
            @RequestParam double startLat,
            @RequestParam double startLng,
            @RequestParam double endLat,
            @RequestParam double endLng,
            @RequestParam(defaultValue = "1.0") double maxDistanceKm
    ) {
        log.info(
                "GET /api/route-lockers start=({},{}) end=({},{}) maxDist={}km",
                startLat,
                startLng,
                endLat,
                endLng,
                maxDistanceKm
        );

        if (!isValidCoordinate(startLat, startLng) || !isValidCoordinate(endLat, endLng)) {
            return ResponseEntity.badRequest().body(Map.of("error","Nieprawidłowe współrzędne"));
        }

        if (maxDistanceKm <= 0.0 || maxDistanceKm > 10.0) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Maksymalna odległość od trasy musi być większa od 0 i nie większa niż 10 km"
            ));
        }

        GeoPoint start = new GeoPoint(startLat, startLng);
        GeoPoint end = new GeoPoint(endLat, endLng);

        double routeLengthKm = GeoUtils.haversineDistanceKm(
                start.getLat(),
                start.getLng(),
                end.getLat(),
                end.getLng()
        );

        if (routeLengthKm > MAX_ROUTE_LENGTH_KM) {
            String message = "Wybrana trasa ma około "
                    + Math.round(routeLengthKm)
                    + " km. Obecna wersja aplikacji obsługuje trasy do "
                    + Math.round(MAX_ROUTE_LENGTH_KM) + " km";

            log.warn(message);

            return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    message
            ));
        }

        RouteLockersResponse response = lockerRouteService.findLockersAlongRoute(start, end, maxDistanceKm);

        return ResponseEntity.ok(response);
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }
}