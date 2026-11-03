package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class RoomGenerator {

    private RoomGenerator() {
    }

    // 임의 이름의 예약 가능한 객실을 만든다 (최대 2명, 기본 요금 100,000원). 저장하지 않는다
    public static Room generateRoom(Accommodation accommodation) {
        return Room.builder()
                .name("객실-" + UUID.randomUUID().toString().substring(0, 8))
                .basePrice(new BigDecimal("100000.00"))
                .maxCapacity(2)
                .isReservable(true)
                .accommodation(accommodation)
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
        return RoomInventory.builder()
                .inventoryDate(date)
                .price(room.getBasePrice())
                .isStocked(isStocked)
                .room(room)
                .build();
    }
}
