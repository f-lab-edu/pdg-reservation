package com.pdg.reservation.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    // 1. 결제 완료
    PAID("결제 완료"),

    // 취소 진행 중 - PG 취소를 요청하기 전에 이 상태로 바꾼다. 확정까지 끝나면 CANCELED, PG가 거절하면 PAID로 돌아간다
    CANCELING("취소 진행 중"),

    // 2. 결제 취소 (환불 포함)
    CANCELED("결제 취소");

    private final String description;
}