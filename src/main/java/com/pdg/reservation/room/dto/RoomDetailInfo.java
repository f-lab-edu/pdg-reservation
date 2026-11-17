package com.pdg.reservation.room.dto;

import com.pdg.reservation.accommodation.dto.ImageInfo;
import com.pdg.reservation.accommodation.enums.BedType;
import com.pdg.reservation.room.entity.Room;

import java.math.BigDecimal;
import java.util.List;

// 객실 상세. Facade가 엔티티에서 만들어 Controller에 넘긴다
public record RoomDetailInfo(
        long id,
        String name,
        String description,
        int maxCapacity,
        String roomLabel,
        BedType bedType,
        BigDecimal price,
        List<ImageInfo> images) {

    public static RoomDetailInfo from(Room room) {
        return new RoomDetailInfo(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getMaxCapacity(),
                room.getRoomLabel(),
                room.getBedType(),
                room.getBasePrice(),
                room.getImages().stream().map(ImageInfo::from).toList());
    }
}
