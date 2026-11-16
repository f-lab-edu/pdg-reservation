package com.pdg.reservation.room.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.pdg.reservation.accommodation.enums.BedType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;

@Getter
public class RoomSearchResponse {

    private final long id;
    private final String name;
    private final String description;
    private final int maxCapacity;
    private final String roomLabel;
    private final BedType bedType;
    private final String mainImage;

    // 가격 계산 후 값 세팅
    private long totalPrice;
    private double avgPrice;

    // 조회 쿼리용. 가격은 쿼리가 아니라 RoomPriceService가 계산해서 updatePrice로 채운다
    @QueryProjection
    public RoomSearchResponse(
            long id, String name, String description, int maxCapacity,
            String roomLabel, BedType bedType, String mainImage
    ) {
        this(id, name, description, maxCapacity, roomLabel, bedType, 0L, 0.0, mainImage);
    }

    // 공개 생성자가 둘이라 JSON 역직렬화(테스트 클라이언트)가 어느 생성자를 쓸지 지정한다
    @JsonCreator
    @Builder
    public RoomSearchResponse(
            long id, String name, String description, int maxCapacity,
            String roomLabel, BedType bedType, long totalPrice, double avgPrice,
            String mainImage
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxCapacity = maxCapacity;
        this.roomLabel = roomLabel;
        this.bedType = bedType;
        this.totalPrice = totalPrice;
        this.avgPrice = avgPrice;
        this.mainImage = mainImage;
    }

    public void updatePrice(long totalPrice, double avgPrice) {
        this.totalPrice = totalPrice;
        this.avgPrice = avgPrice;
    }
}
