package com.inpost.route_finder.client;

import com.inpost.route_finder.model.GeoPoint;
import com.inpost.route_finder.model.Locker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InPostClient {

    private static final Logger log = LoggerFactory.getLogger(InPostClient.class);
    private static final int MAX_API_DISTANCE_METERS = 50_000;

    private static final int MAX_PAGES_PER_SAMPLE_POINT = 3;

    @Value("${inpost.api.base-url}")
    private String baseUrl;

    @Value("${inpost.api.page-size:500}")
    private int pageSize;

    private final RestTemplate restTemplate;

    public InPostClient() {
        this.restTemplate = new RestTemplate();
    }


    //Pobiera paczkomaty w pobliżu konkretnego punktu.
    public List<Locker> fetchNearbyLockers(GeoPoint point, int radiusMeters) {
        int safeRadiusMeters = Math.min(Math.max(radiusMeters, 1), MAX_API_DISTANCE_METERS);

        Map<String, Locker> uniqueLockers = new LinkedHashMap<>();

        int currentPage = 1;
        int totalPages = 1;

        do {
            InPostPageResponse response = fetchNearbyPage(point, safeRadiusMeters, currentPage);

            if (response == null || response.getItems() == null) {
                break;
            }

            totalPages = Math.max(response.getTotalPages(), currentPage);

            response.getItems().stream()
                    .map(this::mapToLocker)
                    .filter(locker -> locker != null)
                    .forEach(locker -> uniqueLockers.put(locker.getId(), locker));

            currentPage++;
        } while (currentPage <= totalPages && currentPage <= MAX_PAGES_PER_SAMPLE_POINT);

        return new ArrayList<>(uniqueLockers.values());
    }

    private InPostPageResponse fetchNearbyPage(GeoPoint point, int radiusMeters, int page) {
        String relativePoint = point.getLat() + "," + point.getLng();

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/points")
                .queryParam("type", "parcel_locker")
                .queryParam("relative_point", relativePoint)
                .queryParam("max_distance", radiusMeters)
                .queryParam("page", page)
                .queryParam("per_page", pageSize)
                .queryParam("sort_by", "distance_to_relative_point")
                .queryParam("sort_order", "asc")
                .queryParam("fields", "name,location,address_details")
                .toUriString();

        log.debug("GET {}", url);

        try {
            return restTemplate.getForObject(url, InPostPageResponse.class);
        } catch (Exception e) {
            log.error(
                    "Błąd pobierania paczkomatów w pobliżu {},{} promień={}m strona={}: {}",
                    point.getLat(),
                    point.getLng(),
                    radiusMeters,
                    page,
                    e.getMessage()
            );
            return null;
        }
    }

    private Locker mapToLocker(InPostPoint point) {
        if (point == null || point.getLocation() == null) {
            return null;
        }

        String name = point.getName();

        if (name == null || name.isBlank()) {
            return null;
        }

        String address = buildAddress(point.getAddressDetails());
        String city = point.getAddressDetails() != null && point.getAddressDetails().getCity() != null
                ? point.getAddressDetails().getCity()
                : "";

        return new Locker(
                name,
                name,
                address,
                city,
                point.getLocation().getLat(),
                point.getLocation().getLng()
        );
    }

    private String buildAddress(InPostPoint.AddressDetails details) {
        if (details == null) {
            return "Brak adresu";
        }

        StringBuilder sb = new StringBuilder();

        if (details.getStreet() != null && !details.getStreet().isBlank()) {
            sb.append(details.getStreet());
        }

        if (details.getBuildingNumber() != null && !details.getBuildingNumber().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(details.getBuildingNumber());
        }

        if (details.getFlatNumber() != null && !details.getFlatNumber().isBlank()) {
            sb.append("/").append(details.getFlatNumber());
        }

        if (details.getPostCode() != null && !details.getPostCode().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(details.getPostCode());
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? "Brak adresu" : result;
    }
}