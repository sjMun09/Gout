package com.gout.dto.response;

import com.gout.entity.Hospital;
import lombok.Getter;

import java.util.List;

@Getter
public class HospitalResponse {

    private final String id;
    private final String name;
    private final String address;
    private final String phone;
    private final List<String> departments;
    private final String operatingHours;
    private final Double latitude;
    private final Double longitude;
    private final Double distanceMeters;
    private final String source;
    private final String kakaoPlaceId;
    private final String placeUrl;

    private HospitalResponse(String id,
                             String name,
                             String address,
                             String phone,
                             List<String> departments,
                             String operatingHours,
                             Double latitude,
                             Double longitude,
                             Double distanceMeters,
                             String source,
                             String kakaoPlaceId,
                             String placeUrl) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.departments = departments;
        this.operatingHours = operatingHours;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
        this.source = source;
        this.kakaoPlaceId = kakaoPlaceId;
        this.placeUrl = placeUrl;
    }

    public static HospitalResponse of(Hospital h, Double distance) {
        return new HospitalResponse(
                h.getId(),
                h.getName(),
                h.getAddress(),
                h.getPhone(),
                h.getDepartments(),
                h.getOperatingHours(),
                h.getLatitude(),
                h.getLongitude(),
                distance,
                "DB",
                null,
                null
        );
    }

    public static HospitalResponse fromNative(String id,
                                              String name,
                                              String address,
                                              String phone,
                                              Double latitude,
                                              Double longitude,
                                              List<String> departments,
                                              String operatingHours,
                                              Double distanceMeters) {
        return new HospitalResponse(
                id,
                name,
                address,
                phone,
                departments,
                operatingHours,
                latitude,
                longitude,
                distanceMeters,
                "DB",
                null,
                null
        );
    }

    public static HospitalResponse fromKakao(String kakaoPlaceId,
                                             String name,
                                             String address,
                                             String phone,
                                             Double latitude,
                                             Double longitude,
                                             List<String> departments,
                                             String placeUrl,
                                             Double distanceMeters) {
        return new HospitalResponse(
                "kakao:" + kakaoPlaceId,
                name,
                address,
                phone,
                departments,
                null,
                latitude,
                longitude,
                distanceMeters,
                "KAKAO",
                kakaoPlaceId,
                placeUrl
        );
    }
}
