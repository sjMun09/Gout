package com.gout.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HospitalSearchRequest {

    private String keyword;
    private Double lat;
    private Double lng;
    private int radius = 5000;
    private int page = 0;
    private int size = 20;

    public boolean hasLocation() {
        return lat != null && lng != null;
    }

    public int getOffset() {
        return Math.max(0, page) * Math.max(1, size);
    }
}
