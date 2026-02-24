package com.pdg.reservation.reservation.service;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.pdg.reservation.reservation.repository.ReservationRedisRepository;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;
import com.pdg.reservation.room.repository.RoomInventoryRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomInventoryRepository roomInventoryRepository; // 💡 재고 조회를 위해 필요
    private final RoomRepository roomRepository;
    private final ReservationRedisRepository reservationRedisRepository;

    @DistributedLock(key = "'room:' + #roomId")
    @Transactional
    public ReservationResponse reserve(Long roomId, Member member, ReservationRequest reserveRequest) {
        Room room = roomRepository.findById(reserveRequest.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<RoomInventory> inventories = roomInventoryRepository.findAllByRoomAndDateRange(
                reserveRequest.getRoomId(),
                reserveRequest.getCheckInDate(),
                reserveRequest.getCheckOutDate()
        );

        validateInventoryAndPrice(member, reserveRequest, room, inventories);

        Reservation reservation = reserveRequest.toEntity(member, room);
        reservationRepository.save(reservation);

        inventories.forEach(inventory -> inventory.updateStockStatus(false));

        reservationRedisRepository.savePaymentTimeout(reservation.getId(), 1);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public void rollbackReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));

        // 이미 결제가 완료되었거나 취소된 건은 무시
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            log.info("이미 처리된 예약입니다. 롤백을 중단합니다: id={}", reservationId);
            return;
        }

        // 1. 결제 만료로 인한 취소, 상태 변경
        reservation.expired();

        // 2. 재고 복구
        List<RoomInventory> inventories = roomInventoryRepository.findAllByRoomAndDateRange(
                reservation.getRoom().getId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );

        inventories.forEach(inventory -> inventory.updateStockStatus(true)); // 재고 복구!
        log.info("예약 롤백 성공: ReservationId={}", reservationId);
    }

    private void validateInventoryAndPrice(Member member, ReservationRequest request, Room room, List<RoomInventory> inventories) {
        BigDecimal basePrice = room.getBasePrice();

        // [검증] 숙박 인원 수 검증
        if (request.getGuestCount() > room.getMaxCapacity()) {
            log.warn("수용 인원 초과: roomId={}, maxCapacity={}, requestGuestCount={}", room.getId(), room.getMaxCapacity(), request.getGuestCount());
            throw new CustomException(ErrorCode.RESERVE_EXCEED_MAX_CAPACITY);
        }

        // [검증] 숙박 일수와 DB 데이터 개수 대조
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (inventories.size() != nights) {
            log.warn("날짜 불일치 발생 : roomId={} , checkInDate={} , checkOutDate={}", room.getId(), request.getCheckInDate(), request.getCheckOutDate());
            throw new CustomException(ErrorCode.RESERVE_INVALID_PERIOD);
        }

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



}
