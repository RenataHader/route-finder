package com.inpost.route_finder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteLockersResponse {
    private List<RouteLockerResult> lockers;
    private boolean expandedSearchUsed;
    private double requestedCorridorKm;
    private double usedCorridorKm;
    private String message;
}