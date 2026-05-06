package com.inpost.route_finder.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InPostPageResponse {

    private List<InPostPoint> items;

    @JsonProperty("total_pages")
    private int totalPages;
}