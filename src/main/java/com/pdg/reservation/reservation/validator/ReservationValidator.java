package com.pdg.reservation.reservation.validator;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ReservationValidator {

    public void validate(Member member, ReservationRequest request, Room room, List<RoomInventory> inventories) {

        validateGuestCountCapacity(request, room);
        vakudatePeriod(request, room, inventories);
        validateStockAndPrice(member, request, room, inventories);
    }

    private void validateStockAndPrice(Member member, ReservationRequest request, Room room, List<RoomInventory> inventories) {
        BigDecimal basePrice = room.getBasePrice();
        
        // [검증] 품절 및 가격 합산
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        for (RoomInventory inv : inventories) {
            if (!inv.isStocked()) {
                log.warn("이미 예약된 객실 존재 : roomId={}, inventoryDate={}", room.getId(), inv.getInventoryDate());
                throw new CustomException(ErrorCode.RESERVE_ALREADY_BOOKED);
            }
            calculatedTotal = calculatedTotal.add(Optional.ofNullable(inv.getPrice()).orElse(basePrice));
        }

        BigDecimal finalCalculatedTotal = member.getGrade().calculateDiscountRate(calculatedTotal);

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
