package com.pdg.reservation.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    // 1. 결제 완료
    PAID("결제 완료"),

    // 2. 결제 취소 (환불 포함)
    CANCELED("결제 취소");

    private final String description;
}