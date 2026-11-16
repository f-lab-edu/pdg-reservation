package com.pdg.reservation.reservation.validator;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;
import com.pdg.reservation.room.price.StayPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
public class ReservationValidator {

    public void validate(Member member, ReservationRequest request, Room room, List<RoomInventory> inventories, StayPrice stayPrice) {

        validateReservable(room);
        validateGuestCountCapacity(request, room);
        vakudatePeriod(request, room, inventories);
        validateStockAndPrice(member, request, room, inventories, stayPrice);
    }

    private void validateReservable(Room room) {
        // [검증] 예약 가능한 객실이고 숙소가 운영 중인지
        if (!room.isReservable() || !room.getAccommodation().isOperational()) {
            log.warn("예약할 수 없는 객실 : roomId={}, isReservable={}, accommodationOperational={}",
                    room.getId(), room.isReservable(), room.getAccommodation().isOperational());
            throw new CustomException(ErrorCode.RESERVE_UNAVAILABLE_ROOM);
        }
    }

    private void validateStockAndPrice(Member member, ReservationRequest request, Room room, List<RoomInventory> inventories, StayPrice stayPrice) {
        // [검증] 품절
        for (RoomInventory inv : inventories) {
            if (!inv.isStocked()) {
                log.warn("이미 예약된 객실 존재 : roomId={}, inventoryDate={}", room.getId(), inv.getInventoryDate());
                throw new CustomException(ErrorCode.RESERVE_ALREADY_BOOKED);
            }
        }

        // [검증] 날짜별 가격 합계(StayPriceCalculator가 계산)에 등급 할인을 적용한다
        BigDecimal finalCalculatedTotal = member.getGrade().calculateDiscountRate(stayPrice.total());

        // [검증] 요청 가격과 서버 계산 가격 대조
        if (finalCalculatedTotal.compareTo(request.getTotalPrice()) != 0) {
            log.warn("결제 금액 불일치 발생 : roomId={}, ClientTotalPrice={}, ServerTotalPrice={}", room.getId(), request.getTotalPrice(), finalCalculatedTotal);
            throw new CustomException(ErrorCode.RESERVE_PRICE_MISMATCH);
        }
    }

    private void vakudatePeriod(ReservationRequest request, Room room, List<RoomInventory> inventories) {
        // [검증] 숙박 일수와 DB 데이터 개수 대조
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (inventories.size() != nights) {
            log.warn("날짜 불일치 발생 : roomId={} , checkInDate={} , checkOutDate={}", room.getId(), request.getCheckInDate(), request.getCheckOutDate());
            throw new CustomException(ErrorCode.RESERVE_INVALID_PERIOD);
        }
    }

    private void validateGuestCountCapacity(ReservationRequest request, Room room) {
        // [검증] 숙박 인원 수 검증
        if (request.getGuestCount() > room.getMaxCapacity()) {
            log.warn("수용 인원 초과: roomId={}, maxCapacity={}, requestGuestCount={}", room.getId(), room.getMaxCapacity(), request.getGuestCount());
            throw new CustomException(ErrorCode.RESERVE_EXCEED_MAX_CAPACITY);
        }
    }


}
