package com.pdg.reservation.payment.dto;

import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// 결제 결과. Facade가 엔티티에서 만들어 Controller에 넘긴다
public record PaymentInfo(
        UUID paymentId,
        UUID reservationId,
        BigDecimal amount,
        PaymentMethod method,
        String pgTransactionNumber,
        PaymentStatus status,
        LocalDateTime paidAt) {

    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
                payment.getPublicId(),
                payment.getReservation().getPublicId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getPgTransactionNumber(),
                payment.getStatus(),
                payment.getPaidAt());
    }
}
