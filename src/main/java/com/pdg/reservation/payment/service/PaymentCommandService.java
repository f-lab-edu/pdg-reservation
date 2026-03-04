package com.pdg.reservation.payment.service;


import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.repository.PaymentRepository;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCommandService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;

    @Transactional
    public PaymentResponse pay(Long reservationId, String pgTransactionNumber, PaymentRequest paymentRequest) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));

        Payment payment = paymentRequest.toEntity(reservation, pgTransactionNumber);
        paymentRepository.save(payment);
        reservationService.updateConfirmStatus(reservation);
        return PaymentResponse.from(payment);
    }

}
