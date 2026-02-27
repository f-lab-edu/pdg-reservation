package com.pdg.reservation.room.dto;

import com.pdg.reservation.accommodation.dto.ImageResponse;
import com.pdg.reservation.accommodation.enums.BedType;
import com.pdg.reservation.room.entity.Room;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomDetailResponse {

    private final long id;
    private final String name;
    private final String description;
    private final int maxCapacity;
    private final String roomLabel;
    private final BedType bedType;
    private final BigDecimal price;
    private final List<ImageResponse> images;

    public static RoomDetailResponse from(Room room) {
        return RoomDetailResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .maxCapacity(room.getMaxCapacity())
                .roomLabel(room.getRoomLabel())
                .bedType(room.getBedType())
                .price(room.getBasePrice())
                .images(room.getImages()
                        .stream()
                        .map(ImageResponse::from)
                        .toList())
                .build();
    }




}
