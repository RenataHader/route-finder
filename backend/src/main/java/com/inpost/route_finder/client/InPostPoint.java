package com.inpost.route_finder.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InPostPoint {

    private String name;

    @JsonProperty("address_details")
    private AddressDetails addressDetails;

    private Location location;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressDetails {
        private String city;
        private String street;

        @JsonProperty("building_number")
        private String buildingNumber;

        @JsonProperty("flat_number")
        private String flatNumber;

        @JsonProperty("post_code")
        private String postCode;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {

        @JsonProperty("latitude")
        private double lat;

        @JsonProperty("longitude")
        private double lng;
    }
}