package com.pdg.reservation.reservation.validator;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.price.StayPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class ReservationValidator {

    static final int MAX_STAY_NIGHTS = 14;

    // blocked: 투숙 기간에 운영자가 막은 날짜가 있는가, occupied: 이미 점유된 밤이 있는가
    public void validate(Member member, ReservationRequest request, Room room,
                         boolean blocked, boolean occupied, StayPrice stayPrice) {

        validateReservable(room);
        validateGuestCountCapacity(request, room);
        validatePeriod(request, room, blocked);
        validateNotOccupied(room, request, occupied);
        validatePrice(member, request, room, stayPrice);
    }

    private void validateReservable(Room room) {
        // [검증] 예약 가능한 객실이고 숙소가 운영 중인지
        if (!room.isReservable() || !room.getAccommodation().isOperational()) {
            log.warn("예약할 수 없는 객실 : roomId={}, isReservable={}, accommodationOperational={}",
                    room.getId(), room.isReservable(), room.getAccommodation().isOperational());
            throw new CustomException(ErrorCode.RESERVE_UNAVAILABLE_ROOM);
        }
    }

    private void validateGuestCountCapacity(ReservationRequest request, Room room) {
        // [검증] 숙박 인원 수 검증
        if (request.getGuestCount() > room.getMaxCapacity()) {
            log.warn("수용 인원 초과: roomId={}, maxCapacity={}, requestGuestCount={}", room.getId(), room.getMaxCapacity(), request.getGuestCount());
            throw new CustomException(ErrorCode.RESERVE_EXCEED_MAX_CAPACITY);
        }
    }

    private void validatePeriod(ReservationRequest request, Room room, boolean blocked) {
        // [검증] 최대 숙박 일수, 운영자가 막은 날짜
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights > MAX_STAY_NIGHTS || blocked) {
            log.warn("예약할 수 없는 기간 : roomId={}, checkInDate={}, checkOutDate={}, nights={}, blocked={}",
                    room.getId(), request.getCheckInDate(), request.getCheckOutDate(), nights, blocked);
            throw new CustomException(ErrorCode.RESERVE_INVALID_PERIOD);
        }
    }

    private void validateNotOccupied(Room room, ReservationRequest request, boolean occupied) {
        // [검증] 이미 예약된 밤
        if (occupied) {
            log.warn("이미 예약된 객실 : roomId={}, checkInDate={}, checkOutDate={}",
                    room.getId(), request.getCheckInDate(), request.getCheckOutDate());
            throw new CustomException(ErrorCode.RESERVE_ALREADY_BOOKED);
        }
    }

    private void validatePrice(Member member, ReservationRequest request, Room room, StayPrice stayPrice) {
        // [검증] 날짜별 가격 합계에 등급 할인을 적용한 값과 요청 가격 대조
        BigDecimal finalCalculatedTotal = member.getGrade().calculateDiscountRate(stayPrice.total());

        if (finalCalculatedTotal.compareTo(request.getTotalPrice()) != 0) {
            log.warn("결제 금액 불일치 발생 : roomId={}, ClientTotalPrice={}, ServerTotalPrice={}", room.getId(), request.getTotalPrice(), finalCalculatedTotal);
            throw new CustomException(ErrorCode.RESERVE_PRICE_MISMATCH);
        }
    }
}
