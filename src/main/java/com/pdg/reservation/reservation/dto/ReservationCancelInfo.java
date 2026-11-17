package com.pdg.reservation.reservation.dto;

import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// 예약 취소 결과(환불된 결제 정보)
public record ReservationCancelInfo(
        UUID paymentId,
        UUID reservationId,
        BigDecimal amount,
        PaymentMethod method,
        String pgTransactionNumber,
        PaymentStatus status,
        LocalDateTime canceledAt) {

    public static ReservationCancelInfo from(Payment payment) {
        return new ReservationCancelInfo(
                payment.getPublicId(),
                payment.getReservation().getPublicId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getPgTransactionNumber(),
                payment.getStatus(),
                LocalDateTime.now());
    }
}
