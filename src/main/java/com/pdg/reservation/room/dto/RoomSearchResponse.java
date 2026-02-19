package com.pdg.reservation.room.dto;

import com.pdg.reservation.accommodation.enums.BedType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
    private final long totalPrice;
    private final double avgPrice;
    private final String mainImage;

    @QueryProjection
    public RoomSearchResponse(
            long id, String name, String description, int maxCapacity,
            String roomLabel, BedType bedType, long totalPrice, double avgPrice,
            String mainImage

    ){
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



}
