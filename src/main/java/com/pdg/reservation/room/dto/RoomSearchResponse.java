package com.pdg.reservation.room.dto;

import com.pdg.reservation.accommodation.enums.BedType;
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

    // 가격 계산 후 값 세팅. 조회 쿼리는 자리 채움 값(0)을 넣고, RoomPriceService가 계산한 값을 updatePrice로 채운다
    private long totalPrice;
    private double avgPrice;

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

    public static RoomSearchResponse from(RoomSearchInfo info) {
        return RoomSearchResponse.builder()
                .id(info.id())
                .name(info.name())
                .description(info.description())
                .maxCapacity(info.maxCapacity())
                .roomLabel(info.roomLabel())
                .bedType(info.bedType())
                .totalPrice(info.totalPrice())
                .avgPrice(info.avgPrice())
                .mainImage(info.mainImage())
                .build();
    }
}
