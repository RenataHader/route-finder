package com.inpost.route_finder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Locker {
    private String id;
    private String name;
    private String address;
    private String city;
    private double lat;
    private double lng;
}