package com.pdg.reservation.room.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// 한 번의 투숙(체크인~체크아웃 전날까지)의 날짜별 가격과 합계. 가격은 등급 할인 전이다.
public record StayPrice(List<NightPrice> nights) {

    private static final int AVERAGE_SCALE = 4;

    public BigDecimal total() {
        return nights.stream()
                .map(NightPrice::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal averagePerNight() {
        return total().divide(BigDecimal.valueOf(nights.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    // 원 단위로 바꿀 때의 반올림은 이 메서드들 한 곳에서만 한다
    public long totalWon() {
        return total().setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public long averageWon() {
        return averagePerNight().setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
