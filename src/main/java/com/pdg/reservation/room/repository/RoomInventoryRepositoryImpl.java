package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomInventory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.room.entity.QRoomInventory.roomInventory;

@RequiredArgsConstructor
public class RoomInventoryRepositoryImpl implements RoomInventoryRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<RoomInventory> findAllByRoomAndDateRange(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {

        return jpaQueryFactory.select(roomInventory)
                .from(roomInventory)
                .where(
                        roomInventory.room.id.eq(roomId),
                        roomInventory.inventoryDate.goe(checkInDate),
                        roomInventory.inventoryDate.lt(checkOutDate)
                )
                .orderBy(roomInventory.inventoryDate.asc())
                .fetch();
    }
}
