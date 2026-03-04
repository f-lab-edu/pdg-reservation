package com.pdg.reservation.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    // 1. 일반 결제
    CARD("신용카드", true),
    // 2. 간편 결제
    KAKAO_PAY("카카오페이", false),
    NAVER_PAY("네이버페이", false),
    TOSS_PAY("토스페이", false),
    ;

    private final String description;
    private final boolean requiresCardInfo; // 카드 정보가 필수인지 여부

    public boolean isCardInfoRequired() {
        return this.requiresCardInfo;
    }

}
