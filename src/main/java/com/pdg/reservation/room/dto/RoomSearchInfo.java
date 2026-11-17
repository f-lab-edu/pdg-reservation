package com.pdg.reservation.room.dto;

import com.pdg.reservation.accommodation.enums.BedType;
import com.querydsl.core.annotations.QueryProjection;

// 객실 검색 한 건. Repository가 쿼리에서 바로 만든다.
// 가격은 쿼리가 아니라 Facade가 RoomPriceService로 계산해서 채운다(쿼리는 자리 채움 값을 넣는다)
public record RoomSearchInfo(
        long id,
        String name,
        String description,
        int maxCapacity,
        String roomLabel,
        BedType bedType,
        long totalPrice,
        double avgPrice,
        String mainImage) {

    @QueryProjection
    public RoomSearchInfo {
    }

    public RoomSearchInfo withPrice(long totalPrice, double avgPrice) {
        return new RoomSearchInfo(id, name, description, maxCapacity, roomLabel, bedType, totalPrice, avgPrice, mainImage);
    }
}
