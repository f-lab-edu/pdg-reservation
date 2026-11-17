package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.pdg.reservation.accommodation.entity.Address;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

// 숙소 상세. Redis 캐시에 저장되는 값이라 JSON 역직렬화가 되도록 기본 생성자를 둔다
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccommodationDetailInfo {

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
    private List<ImageInfo> images;

    public static AccommodationDetailInfo from(Accommodation accommodation) {
        return AccommodationDetailInfo.builder()
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
                        .map(ImageInfo::from)
                        .toList())
                .build();
    }
}
