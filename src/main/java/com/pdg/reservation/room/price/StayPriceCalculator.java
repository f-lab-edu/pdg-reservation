package com.pdg.reservation.room.price;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 날짜별 가격 계산 규칙을 한 곳에 모은다. 예약 검증, 객실 목록, 숙소 검색이 모두 이 계산을 쓴다.
// 기본 요금과 다른 날짜의 특별 요금만 받고, 특별 요금이 없는 밤은 기본 요금이다.
public final class StayPriceCalculator {

    private StayPriceCalculator() {
    }

    // 체크인부터 체크아웃 전날까지의 밤마다 가격을 정한다. 체크아웃 날과 기간 밖의 특별 요금은 쓰지 않는다
    public static StayPrice calculate(
            BigDecimal basePrice, LocalDate checkInDate, LocalDate checkOutDate, Map<LocalDate, BigDecimal> specialPrices) {
        List<NightPrice> nights = checkInDate.datesUntil(checkOutDate)
                .map(date -> new NightPrice(date, specialPrices.getOrDefault(date, basePrice)))
                .toList();
        return new StayPrice(nights);
    }
}
