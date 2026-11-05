package com.pdg.reservation.reservation.service;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.service.PaymentService;
import com.pdg.reservation.reservation.dto.ReservationCancelRequest;
import com.pdg.reservation.reservation.dto.ReservationCancelResponse;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.room.service.RoomInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationCancelService {

    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;
    private final RoomInventoryService roomInventoryService;
    private final ReservationService reservationService;

    @DistributedLock(key = "'reserveCancel:reservationId:' + #reservationId")
    @Transactional
    public ReservationCancelResponse processCancel(Long memberId, UUID reservationId, ReservationCancelRequest cancelRequest) {
        Reservation reservation = reservationRepository.findByPublicIdAndMemberId(reservationId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));

        // 취소할 수 없는 예약에는 PG 취소를 보내지 않도록 검증을 먼저 한다
        reservation.validateCancelable();

        Payment payment = reservation.getPayment();
        paymentService.cancelPayment(payment, cancelRequest.getCancelReason());

        reservationService.updateCancelStatus(reservation);
        roomInventoryService.restoreStock(
                reservation.getRoom().getId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );
        return ReservationCancelResponse.from(payment);
    }
}
