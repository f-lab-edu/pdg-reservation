package com.pdg.reservation.reservation.service;

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

// 예약 생성 규칙. 회원, 객실, 예약 기간에 걸쳐 있어 한 엔티티에 넣을 수 없는 판단만 모았다. 조회하지 않고 인자만 받는다.
// 객실 하나로 판단되는 규칙(예약 가능 여부, 수용 인원)은 Room이 한다.
@Slf4j
@Component
public class ReservationDomainService {

    static final int MAX_STAY_NIGHTS = 14;

    // blocked: 투숙 기간에 운영자가 막은 날짜가 있는가, occupied: 이미 점유된 밤이 있는가
    // 검사 순서가 에러 코드의 우선순위다: 예약 가능 → 인원 → 기간·차단일 → 점유됨 → 가격
    public void validateReserve(Member member, ReservationRequest request, Room room,
                                boolean blocked, boolean occupied, StayPrice stayPrice) {
        room.validateReservable();
        room.validateCapacity(request.getGuestCount());
        validatePeriod(request, room, blocked);
        validateNotOccupied(request, room, occupied);
        validatePrice(member, request, room, stayPrice);
    }

    private void validatePeriod(ReservationRequest request, Room room, boolean blocked) {
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights > MAX_STAY_NIGHTS || blocked) {
            log.warn("예약할 수 없는 기간 : roomId={}, checkInDate={}, checkOutDate={}, nights={}, blocked={}",
                    room.getId(), request.getCheckInDate(), request.getCheckOutDate(), nights, blocked);
            throw new CustomException(ErrorCode.RESERVE_INVALID_PERIOD);
        }
    }

    private void validateNotOccupied(ReservationRequest request, Room room, boolean occupied) {
        if (occupied) {
            log.warn("이미 예약된 객실 : roomId={}, checkInDate={}, checkOutDate={}",
                    room.getId(), request.getCheckInDate(), request.getCheckOutDate());
            throw new CustomException(ErrorCode.RESERVE_ALREADY_BOOKED);
        }
    }

    private void validatePrice(Member member, ReservationRequest request, Room room, StayPrice stayPrice) {
        BigDecimal finalCalculatedTotal = member.getGrade().calculateDiscountRate(stayPrice.total());
        if (finalCalculatedTotal.compareTo(request.getTotalPrice()) != 0) {
            log.warn("결제 금액 불일치 발생 : roomId={}, ClientTotalPrice={}, ServerTotalPrice={}",
                    room.getId(), request.getTotalPrice(), finalCalculatedTotal);
            throw new CustomException(ErrorCode.RESERVE_PRICE_MISMATCH);
        }
    }
}
