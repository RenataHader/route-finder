package com.inpost.route_finder.util;

import com.inpost.route_finder.model.GeoPoint;

public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double WEB_MERCATOR_RADIUS_M = 6378137.0;
    private static final double MAX_MERCATOR_LAT = 85.05112878;

    private GeoUtils() {}

    public static double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }


     //Liczy odległość punktu od odcinka start-end w projekcji Web Mercator,
    public static double distancePointToSegmentKm(GeoPoint point, GeoPoint start, GeoPoint end) {
        double[] p = toWebMercatorMeters(point);
        double[] a = toWebMercatorMeters(start);
        double[] b = toWebMercatorMeters(end);

        double dx = b[0] - a[0];
        double dy = b[1] - a[1];

        double segmentLengthSq = dx * dx + dy * dy;

        if (segmentLengthSq == 0.0) {
            return haversineDistanceKm(
                    point.getLat(),
                    point.getLng(),
                    start.getLat(),
                    start.getLng()
            );
        }

        double t = ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / segmentLengthSq;
        double clampedT = Math.max(0.0, Math.min(1.0, t));

        double closestX = a[0] + clampedT * dx;
        double closestY = a[1] + clampedT * dy;

        double distanceXMeters = p[0] - closestX;
        double distanceYMeters = p[1] - closestY;

        double mercatorDistanceMeters = Math.sqrt(
                distanceXMeters * distanceXMeters
                        + distanceYMeters * distanceYMeters
        );

        double correction = Math.cos(Math.toRadians(point.getLat()));
        double correctedMeters = mercatorDistanceMeters * correction;

        return correctedMeters / 1000.0;
    }


    //Zwraca położenie punktu względem odcinka start-end
    //t < 0  → punkt jest przed startem
    //0-1    → punkt jest między startem a końcem
    //t > 1  → punkt jest za końcem
    public static double projectionFactor(GeoPoint point, GeoPoint start, GeoPoint end) {
        double[] p = toWebMercatorMeters(point);
        double[] a = toWebMercatorMeters(start);
        double[] b = toWebMercatorMeters(end);

        double dx = b[0] - a[0];
        double dy = b[1] - a[1];

        double segmentLengthSq = dx * dx + dy * dy;

        if (segmentLengthSq == 0.0) {
            return 0.0;
        }

        return ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / segmentLengthSq;
    }

    private static double[] toWebMercatorMeters(GeoPoint point) {
        double lat = clamp(point.getLat(), -MAX_MERCATOR_LAT, MAX_MERCATOR_LAT);
        double lng = point.getLng();

        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        double x = WEB_MERCATOR_RADIUS_M * lngRad;
        double y = WEB_MERCATOR_RADIUS_M * Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0));

        return new double[]{x, y};
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}