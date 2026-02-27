package com.pdg.reservation.reservation.service;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.pdg.reservation.reservation.event.ReservationRollbackEvent;
import com.pdg.reservation.reservation.repository.ReservationRedisRepository;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.reservation.validator.ReservationValidator;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;
import com.pdg.reservation.room.repository.RoomInventoryRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ReservationValidator reservationValidator;
    private final ApplicationEventPublisher eventPublisher;

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

        reservationValidator.validate(member, reserveRequest, room, inventories);

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
        eventPublisher.publishEvent(new ReservationRollbackEvent(reservationId));
    }



}
