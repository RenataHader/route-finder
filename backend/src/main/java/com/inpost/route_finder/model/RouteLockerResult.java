package com.inpost.route_finder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteLockerResult {
    private Locker locker;
    private double distanceFromRouteKm;
    private double projectionFactor;
    private boolean outsideRequestedRange;
    private double requestedCorridorKm;
}