package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.querydsl.core.annotations.QueryProjection;

// 숙소 검색 한 건. Repository가 쿼리에서 바로 만든다. 최저가는 쿼리가 아니라 Facade가 계산해서 채운다
public record AccommodationSearchInfo(
        long id,
        String name,
        AccommodationType type,
        String city,
        String mainImage,
        Long minPrice) {

    @QueryProjection
    public AccommodationSearchInfo(long id, String name, AccommodationType type, String city, String mainImage) {
        this(id, name, type, city, mainImage, null);
    }

    public AccommodationSearchInfo withMinPrice(Long minPrice) {
        return new AccommodationSearchInfo(id, name, type, city, mainImage, minPrice);
    }
}
