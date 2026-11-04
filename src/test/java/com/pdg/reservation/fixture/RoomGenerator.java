package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.AccommodationImage;
import com.pdg.reservation.accommodation.enums.BedType;
import com.pdg.reservation.accommodation.enums.ImageType;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public final class RoomGenerator {

    private RoomGenerator() {
    }

    // 임의 이름·설명·라벨의 예약 가능한 더블 객실을 만든다 (최대 2명, 기본 요금 100,000원). 저장하지 않는다
    public static Room generateRoom(Accommodation accommodation) {
        return generateRoom(accommodation, new BigDecimal("100000.00"));
    }

    // 기본 요금을 지정한 예약 가능한 객실을 만든다 (최대 2명). 저장하지 않는다
    public static Room generateRoom(Accommodation accommodation, BigDecimal basePrice) {
        return Room.builder()
                .name("객실-" + UUID.randomUUID().toString().substring(0, 8))
                .description("설명-" + UUID.randomUUID())
                .roomLabel("라벨-" + UUID.randomUUID().toString().substring(0, 8))
                .bedType(BedType.DOUBLE)
                .basePrice(basePrice)
                .maxCapacity(2)
                .isReservable(true)
                .accommodation(accommodation)
                .build();
    }

    // 이미지 3개를 정렬 순서가 섞인 채(2, 0, 1) 붙인 객실을 만든다. sortOrder 0이 대표 이미지다. 저장하지 않는다
    public static Room generateRoomWithImages(Accommodation accommodation) {
        Room room = generateRoom(accommodation);
        room.addImage(generateImage(2, false));
        room.addImage(generateImage(0, true));
        room.addImage(generateImage(1, false));
        return room;
    }

    // 대표가 아닌 이미지 1개만 붙인 객실을 만든다. 저장하지 않는다
    public static Room generateRoomWithNonMainImage(Accommodation accommodation) {
        Room room = generateRoom(accommodation);
        room.addImage(generateImage(0, false));
        return room;
    }

    private static AccommodationImage generateImage(int sortOrder, boolean main) {
        return AccommodationImage.builder()
                .imageType(ImageType.ROOM)
                .url("https://example.com/" + UUID.randomUUID() + ".jpg")
                .isMain(main)
                .sortOrder(sortOrder)
                .build();
    }

    // 체크인부터 체크아웃 전날까지, 날짜마다 재고가 있는 상태의 재고 행을 만든다. 가격은 객실 기본 요금이다
    public static List<RoomInventory> generateRoomInventories(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        return checkInDate.datesUntil(checkOutDate)
                .map(date -> generateRoomInventory(room, date, true))
                .toList();
    }

    // 하루치 재고 행을 만든다. 재고 여부를 지정하고 가격은 객실 기본 요금이다
    public static RoomInventory generateRoomInventory(Room room, LocalDate date, boolean isStocked) {
        return generateRoomInventory(room, date, isStocked, room.getBasePrice());
    }

    // 하루치 재고 행을 만든다. 재고 여부와 가격을 지정한다
    public static RoomInventory generateRoomInventory(Room room, LocalDate date, boolean isStocked, BigDecimal price) {
        return RoomInventory.builder()
                .inventoryDate(date)
                .price(price)
                .isStocked(isStocked)
                .room(room)
                .build();
    }

    // 체크인부터 하루씩, 지정한 가격으로 재고가 있는 상태의 재고 행을 만든다. 목록의 null은 가격이 없는 밤이다
    public static List<RoomInventory> generateRoomInventoriesWithNightlyPrices(Room room, LocalDate checkInDate, List<Long> prices) {
        return IntStream.range(0, prices.size())
                .mapToObj(i -> generateRoomInventory(room, checkInDate.plusDays(i), true, toPrice(prices.get(i))))
                .toList();
    }

    private static BigDecimal toPrice(Long price) {
        return price == null ? null : BigDecimal.valueOf(price);
    }
}
