package com.pdg.reservation.reservation.service;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationSearchCondition;
import com.pdg.reservation.reservation.dto.ReservationSearchResponse;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.entity.ReservationNight;
import com.pdg.reservation.reservation.event.ReservationRollbackEvent;
import com.pdg.reservation.reservation.repository.ReservationNightRepository;
import com.pdg.reservation.reservation.repository.ReservationRedisRepository;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.price.StayPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationNightRepository reservationNightRepository;
    private final ReservationRedisRepository reservationRedisRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 투숙 기간(체크인 ~ 체크아웃 전날)에 이미 점유된 밤이 있는가
    public boolean isOccupied(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        return reservationNightRepository
                .existsByRoomIdAndStayDateGreaterThanEqualAndStayDateLessThan(roomId, checkInDate, checkOutDate);
    }

    @Transactional
    public Reservation reserve(Member member, Room room, ReservationRequest request, StayPrice stayPrice) {
        Reservation reservation = request.toEntity(member, room);
        reservationRepository.save(reservation);

        // 날짜 오름차순으로 점유한다. (room_id, stay_date) 유니크가 이중 점유를 막는다
        List<ReservationNight> nights = stayPrice.nights().stream()
                .map(nightPrice -> ReservationNight.of(reservation, room, nightPrice))
                .toList();
        saveNights(nights, room.getId());

        reservationRedisRepository.savePaymentTimeout(reservation.getId(), 1);
        return reservation;
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

    public Reservation getById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));
    }

    // 회원이 가진 예약을 외부 식별자로 찾는다. 남의 예약이면 결과가 없다
    public Optional<Reservation> findByPublicIdAndMemberId(UUID publicId, Long memberId) {
        return reservationRepository.findByPublicIdAndMemberId(publicId, memberId);
    }

    public Reservation getByPublicIdAndMemberId(UUID publicId, Long memberId) {
        return findByPublicIdAndMemberId(publicId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));
    }

    // 취소할 수 있는 예약이어야 한다(상태, 체크인 전날까지)
    public Reservation getCancelable(UUID publicId, Long memberId) {
        Reservation reservation = getByPublicIdAndMemberId(publicId, memberId);
        reservation.validateCancelable();
        return reservation;
    }

    public void validatePayable(Reservation reservation, BigDecimal amount) {
        reservation.validatePayable(amount);
    }

    @Transactional
    public void updateConfirmStatus(Reservation reservation) {
        reservation.updateStatusConfirmed();
        reservationRepository.save(reservation);
    }

    // 취소 상태로 바꾸고 점유한 밤을 푼다
    @Transactional
    public void cancel(Reservation reservation) {
        reservation.updateStatusCanceled();
        reservationRepository.save(reservation);
        reservationNightRepository.deleteByReservationId(reservation.getId());
    }

    @Transactional
    public void rollbackReservation(Long reservationId) {
        Reservation reservation = getById(reservationId);

        // 이미 결제가 완료되었거나 취소된 건은 무시
        if (!reservation.isPaymentPending()) {
            log.info("이미 처리된 예약입니다. 롤백을 중단합니다: id={}", reservationId);
            return;
        }

        // 1. 결제 만료로 인한 취소, 상태 변경
        reservation.updateStatusExpired();
        reservationRepository.save(reservation);

        // 2. 점유한 밤 해제
        reservationNightRepository.deleteByReservationId(reservationId);

        eventPublisher.publishEvent(new ReservationRollbackEvent(reservationId));
    }

    // 결제가 끝난 예약의 결제 제한시간 키를 지운다
    public void clearPaymentTimeout(Long reservationId) {
        reservationRedisRepository.deletePaymentTimeout(reservationId);
    }

    public Page<ReservationSearchResponse> search(Long memberId, ReservationSearchCondition condition, Pageable pageable) {
        return reservationRepository.search(memberId, condition, pageable);
    }
}
