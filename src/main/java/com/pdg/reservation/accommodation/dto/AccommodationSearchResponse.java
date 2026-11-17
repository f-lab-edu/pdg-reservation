package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.enums.AccommodationType;
import lombok.Builder;
import lombok.Getter;
@Getter
public class AccommodationSearchResponse {

    private final long id;
    private final String name;
    private final AccommodationType type;
    private final String city;
    private final String mainImage;

    //최저가 계산 후 값 세팅
    private Long minPrice;

    @Builder
    public AccommodationSearchResponse(long id, String name, AccommodationType type, String city, String mainImage) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.city = city;
        this.mainImage = mainImage;
    }

    public void updateMinPrice(Long minPrice) {
        this.minPrice = minPrice;
    }

    public static AccommodationSearchResponse from(AccommodationSearchInfo info) {
        AccommodationSearchResponse response = AccommodationSearchResponse.builder()
                .id(info.id())
                .name(info.name())
                .type(info.type())
                .city(info.city())
                .mainImage(info.mainImage())
                .build();
        response.updateMinPrice(info.minPrice());
        return response;
    }



}
