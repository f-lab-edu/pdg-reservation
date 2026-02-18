package com.pdg.reservation.room.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.pdg.reservation.accommodation.entity.QAccommodation.accommodation;
import static com.pdg.reservation.room.entity.QRoom.room;
import static com.pdg.reservation.room.entity.QRoomInventory.roomInventory;

/*객실 조건 공통 유틸 클래스*/
public class RoomExpressions {

    /**
     * 특정 조건(날짜, 인원)에 예약 가능한 객실인지 확인하는 공통 조건
     */
    public static BooleanExpression isAccAvailable(LocalDate checkInDate, LocalDate checkOutDate, Integer capacity) {

        // 투숙 박수 계산 (예: 24일~26일 = 2박)
        long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        // 가용 객실 존재 여부 체크 서브쿼리
        return JPAExpressions
                .selectOne()
                .from(room)
                .join(roomInventory)
                .on(roomInventory.room.eq(room))
                .where(
                        room.accommodation.eq(accommodation),
                        room.maxCapacity.goe(capacity),
                        roomInventory.isStocked.isTrue(),
                        roomInventory.inventoryDate.goe(checkInDate),    // 체크인부터
                        roomInventory.inventoryDate.lt(checkOutDate)     // 체크아웃 전날까지
                )
                .groupBy(room.id) // 방별로 그룹화하여
                .having(roomInventory.count().eq(stayDays)) // 투숙 기간 '모든 날짜'에 재고가 있는지 확인
                .exists(); // 조건을 만족하는 방이 하나라도 있다면 TRUE
    }

    public static BooleanExpression isRoomAvailable(LocalDate checkInDate, LocalDate checkOutDate) {
        long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        return JPAExpressions
                .selectOne()
                .from(roomInventory)
                .where(
                        roomInventory.room.eq(room),
                        roomInventory.isStocked.isTrue(),
                        roomInventory.inventoryDate.goe(checkInDate),
                        roomInventory.inventoryDate.lt(checkOutDate)
                )
                .groupBy(roomInventory.room.id)
                .having(roomInventory.count().eq(stayDays))
                .exists();
    }
}
