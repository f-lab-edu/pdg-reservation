package com.pdg.reservation.room.repository;

import com.pdg.reservation.room.entity.RoomSpecialPrice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.pdg.reservation.room.entity.QRoomSpecialPrice.roomSpecialPrice;

@RequiredArgsConstructor
public class RoomSpecialPriceRepositoryImpl implements RoomSpecialPriceRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<RoomSpecialPrice> findInStay(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        return findInStay(List.of(roomId), checkInDate, checkOutDate);
    }

    @Override
    public List<RoomSpecialPrice> findInStay(Collection<Long> roomIds, LocalDate checkInDate, LocalDate checkOutDate) {
        return jpaQueryFactory
                .selectFrom(roomSpecialPrice)
                .where(
                        roomSpecialPrice.room.id.in(roomIds),
                        roomSpecialPrice.priceDate.goe(checkInDate),
                        roomSpecialPrice.priceDate.lt(checkOutDate)
                )
                .fetch();
    }
}
