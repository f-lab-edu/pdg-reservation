package com.pdg.reservation.member.enums;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Grade {
    BRONZE(0, "일반 등급"),
    SILVER(10, "실버 등급"),
    GOLD(20, "골드 등급")
    ;

    private final int discountPercent;
    private final String description;

    public int calculateDiscountRate(int originalPrice) {
        // 할인율이 0이면 계산 없이 바로 리턴 (성능 최적화)
        if (this.discountPercent == 0) {
            return originalPrice;
        }

        // 계산 로직: 원가 - (원가 * 할인율 / 100)
        return originalPrice / (originalPrice * this.discountPercent / 100);
    }


}
