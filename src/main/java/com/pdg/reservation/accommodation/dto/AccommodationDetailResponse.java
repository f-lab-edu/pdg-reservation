package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.Address;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import lombok.*;


import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccommodationDetailResponse {

    private Long id;
    private String name;
    private AccommodationType type;
    private String description;
    private List<String> amenities;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private BigDecimal ratingAverage;
    private boolean operational;
    private Address address;
    private List<ImageResponse> images;

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
                .operational(accommodation.isOperational())
                .address(accommodation.getAddress())
                .images(accommodation.getImages()
                        .stream()
                        .map(ImageResponse::from)
                        .toList())
                .build();
    }
}
