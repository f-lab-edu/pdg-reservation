package com.pdg.reservation.reservation.service;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.dto.ReservationSearchCondition;
import com.pdg.reservation.reservation.dto.ReservationSearchResponse;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.pdg.reservation.reservation.event.ReservationRollbackEvent;
import com.pdg.reservation.reservation.repository.ReservationRedisRepository;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.reservation.validator.ReservationValidator;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.reservation.entity.ReservationNight;
import com.pdg.reservation.reservation.repository.ReservationNightRepository;
import com.pdg.reservation.room.repository.RoomBlockedDateRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import com.pdg.reservation.room.price.StayPrice;
import com.pdg.reservation.room.service.RoomPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final ReservationRedisRepository reservationRedisRepository;
    private final ReservationValidator reservationValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final ReservationNightRepository reservationNightRepository;
    private final RoomBlockedDateRepository roomBlockedDateRepository;
    private final RoomPriceService roomPriceService;

    @Transactional
    public ReservationResponse reserve(Long roomId, Member member, ReservationRequest reserveRequest) {
        Room room = roomRepository.findById(reserveRequest.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        LocalDate checkIn = reserveRequest.getCheckInDate();
        LocalDate checkOut = reserveRequest.getCheckOutDate();
        boolean blocked = roomBlockedDateRepository
                .existsByRoomIdAndBlockedDateGreaterThanEqualAndBlockedDateLessThan(roomId, checkIn, checkOut);
        boolean occupied = reservationNightRepository
                .existsByRoomIdAndStayDateGreaterThanEqualAndStayDateLessThan(roomId, checkIn, checkOut);
        StayPrice stayPrice = roomPriceService.calculateStayPrice(room, checkIn, checkOut);
        reservationValidator.validate(member, reserveRequest, room, blocked, occupied, stayPrice);

        Reservation reservation = reserveRequest.toEntity(member, room);
        reservationRepository.save(reservation);

        // 날짜 오름차순으로 점유한다. (room_id, stay_date) 유니크가 이중 점유를 막는다
        List<ReservationNight> nights = stayPrice.nights().stream()
                .map(nightPrice -> ReservationNight.of(reservation, room, nightPrice))
                .toList();
        saveNights(nights, roomId);

        reservationRedisRepository.savePaymentTimeout(reservation.getId(), 1);
        return ReservationResponse.from(reservation);
    }

    // 사전 조회를 동시에 통과한 요청은 여기서 (room_id, stay_date) 유니크로 가려진다. 진 쪽은 409로 응답하고 예약 저장도 함께 롤백된다
    private void saveNights(List<ReservationNight> nights, Long roomId) {
        try {
            reservationNightRepository.saveAll(nights);
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            log.warn("점유 저장 충돌 : roomId={}, cause={}", roomId, e.getClass().getSimpleName());
            throw new CustomException(ErrorCode.RESERVE_ALREADY_BOOKED);
        }
    }

    @Transactional
    public void updateConfirmStatus(Reservation reservation) {
        reservation.updateStatusConfirmed();
        reservationRepository.save(reservation);
    }

    @Transactional
    public void updateCancelStatus(Reservation reservation) {
        reservation.updateStatusCanceled();
        reservationRepository.save(reservation);
    }

    @Transactional
    public void updateExpiredStatus(Reservation reservation) {
        reservation.updateStatusExpired();
        reservationRepository.save(reservation);
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
        updateExpiredStatus(reservation);

        // 2. 점유한 밤 해제
        reservationNightRepository.deleteByReservationId(reservationId);

        eventPublisher.publishEvent(new ReservationRollbackEvent(reservationId));
    }

    public Page<ReservationSearchResponse> search(Long memberId, ReservationSearchCondition condition, Pageable pageable) {
        return reservationRepository.search(memberId, condition, pageable);
    }





}
