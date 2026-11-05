package com.pdg.reservation.reservation.dto;

import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReservationCancelResponse {

    private UUID paymentId;             // 결제의 외부 식별자(public_id)
    private UUID reservationId;         // 예약의 외부 식별자(public_id)
    private BigDecimal amount;          // 실제 환불된 금액
    private PaymentMethod method;       // 결제 수단
    private String pgTransactionNumber; // PG사 승인 번호 (영수증 조회/환불 시 필수) 가상의 UUID
    private PaymentStatus status;       // 결제 상태
    private LocalDateTime canceledAt;   // 결제 완료 시간

    public static ReservationCancelResponse from(Payment payment) {
        return ReservationCancelResponse.builder()
                .paymentId(payment.getPublicId())
                .reservationId(payment.getReservation().getPublicId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .pgTransactionNumber(payment.getPgTransactionNumber())
                .status(payment.getStatus())
                .canceledAt(LocalDateTime.now())
                .build();
    }
}