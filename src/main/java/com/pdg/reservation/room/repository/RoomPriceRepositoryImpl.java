package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomPrice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.pdg.reservation.room.entity.QRoomPrice.roomPrice;

@RequiredArgsConstructor
public class RoomPriceRepositoryImpl implements RoomPriceRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<RoomPrice> findInStay(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        return findInStay(List.of(roomId), checkInDate, checkOutDate);
    }

    @Override
    public List<RoomPrice> findInStay(Collection<Long> roomIds, LocalDate checkInDate, LocalDate checkOutDate) {
        return jpaQueryFactory
                .selectFrom(roomPrice)
                .where(
                        roomPrice.room.id.in(roomIds),
                        roomPrice.priceDate.goe(checkInDate),
                        roomPrice.priceDate.lt(checkOutDate)
                )
                .fetch();
    }
}
