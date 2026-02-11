package com.pdg.reservation.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    // 1. 일반 결제
    CARD("신용카드"),
    // 2. 간편 결제
    KAKAO_PAY("카카오페이"),
    NAVER_PAY("네이버페이"),
    TOSS_PAY("토스페이"),
    ;

    private final String description;
}
