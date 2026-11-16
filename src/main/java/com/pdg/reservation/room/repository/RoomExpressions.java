package com.pdg.reservation.room.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;

import java.time.LocalDate;

import static com.pdg.reservation.accommodation.entity.QAccommodation.accommodation;
import static com.pdg.reservation.reservation.entity.QReservationNight.reservationNight;
import static com.pdg.reservation.room.entity.QRoom.room;
import static com.pdg.reservation.room.entity.QRoomBlockedDate.roomBlockedDate;

/*객실 조건 공통 유틸 클래스*/
public class RoomExpressions {

    /**
     * 숙소에 해당 기간·인원으로 예약 가능한 객실이 하나라도 있는지
     */
    public static BooleanExpression isAccAvailable(LocalDate checkInDate, LocalDate checkOutDate, Integer capacity) {
        return JPAExpressions
                .selectOne()
                .from(room)
                .where(
                        room.accommodation.eq(accommodation),
                        room.maxCapacity.goe(capacity),
                        isRoomAvailable(checkInDate, checkOutDate)
                )
                .exists();
    }

    /**
     * 투숙 기간(체크인 ~ 체크아웃 전날)에 점유된 밤도 막힌 날짜도 없는 객실인지
     */
    public static BooleanExpression isRoomAvailable(LocalDate checkInDate, LocalDate checkOutDate) {
        BooleanExpression notOccupied = JPAExpressions
                .selectOne()
                .from(reservationNight)
                .where(
                        reservationNight.room.eq(room),
                        reservationNight.stayDate.goe(checkInDate),
                        reservationNight.stayDate.lt(checkOutDate)
                )
                .notExists();

        BooleanExpression notBlocked = JPAExpressions
                .selectOne()
                .from(roomBlockedDate)
                .where(
                        roomBlockedDate.room.eq(room),
                        roomBlockedDate.blockedDate.goe(checkInDate),
                        roomBlockedDate.blockedDate.lt(checkOutDate)
                )
                .notExists();

        return notOccupied.and(notBlocked);
    }
}
