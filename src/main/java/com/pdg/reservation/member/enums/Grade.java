package com.pdg.reservation.member.enums;


import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public enum Grade {
    BRONZE(0, "일반 등급"),
    SILVER(10, "실버 등급"),
    GOLD(20, "골드 등급")
    ;

    private final int discountPercent;
    private final String description;

    public BigDecimal calculateDiscountRate(BigDecimal originalPrice) {
        // 할인율이 0이면 계산 없이 바로 리턴 (성능 최적화)
        if (this.discountPercent == 0) {
            return originalPrice;
        }

        // 계산 로직: 원가 * (100 - 할인율) / 100
        BigDecimal discountRate = BigDecimal.valueOf(100 - this.discountPercent);
        return originalPrice.multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR); // 원 단위 절사
    }


}
