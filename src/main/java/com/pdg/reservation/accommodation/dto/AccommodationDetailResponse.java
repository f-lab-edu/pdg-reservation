package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.Address;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccommodationDetailResponse {

    private final Long id;
    private final String name;
    private final AccommodationType type;
    private final String description;
    private final List<String> amenities;
    private final LocalTime checkInTime;
    private final LocalTime checkOutTime;
    private final BigDecimal ratingAverage;
    private final boolean isOperational;
    private final Address address;
    private final List<ImageResponse> images;

    public static AccommodationDetailResponse from(Accommodation accommodation) {
        return AccommodationDetailResponse.builder()
                .id(accommodation.getId())
                .name(accommodation.getName())
                .type(accommodation.getType())
                .description(accommodation.getDescription())
                .amenities(accommodation.getAmenities())
                .checkInTime(accommodation.getCheckInTime())
                .checkOutTime(accommodation.getCheckOutTime())
                .ratingAverage(accommodation.getRatingAverage())
                .isOperational(accommodation.isOperational())
                .address(accommodation.getAddress())
                .images(accommodation.getImages()
                        .stream()
                        .map(ImageResponse::from)
                        .toList())
                .build();
    }
}
