package com.pdg.reservation.payment.service;


import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.event.PaymentConfirmedEvent;
import com.pdg.reservation.payment.repository.PaymentRepository;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCommandService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse pay(Long memberId, Long reservationId, String pgTransactionNumber, PaymentRequest paymentRequest) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));

        Payment payment = paymentRequest.toEntity(reservation, pgTransactionNumber);
        paymentRepository.save(payment);
        reservationService.updateConfirmStatus(reservation);

        eventPublisher.publishEvent(new PaymentConfirmedEvent(
                                        memberId,
                                        reservationId,
                                        UUID.randomUUID().toString(),
                                        LocalDateTime.now()
                                    ));

        return PaymentResponse.from(payment);
    }

}
