package com.pdg.reservation.room.entity;

import com.pdg.reservation.room.price.NightPrice;
import com.pdg.reservation.room.price.StayPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTest {

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000");
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 21);

    private final Room room = Room.builder().basePrice(BASE_PRICE).build();

    @Test
    void 특별_요금이_없으면_모든_밤이_기본_요금이다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(3);

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, List.of());

        // Assert
        assertThat(stayPrice.nights())
                .extracting(NightPrice::price)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("100000"), new BigDecimal("100000"), new BigDecimal("100000"));
        assertThat(stayPrice.total()).isEqualByComparingTo("300000");
    }

    @Test
    void 특별_요금이_있는_밤은_특별_요금이고_나머지는_기본_요금이다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(3);
        List<RoomSpecialPrice> specialPrices = List.of(specialPrice(CHECK_IN.plusDays(1), "150000"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        assertThat(stayPrice.nights())
                .extracting(NightPrice::date)
                .containsExactly(CHECK_IN, CHECK_IN.plusDays(1), CHECK_IN.plusDays(2));
        assertThat(stayPrice.nights())
                .extracting(NightPrice::price)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("100000"), new BigDecimal("150000"), new BigDecimal("100000"));
        assertThat(stayPrice.total()).isEqualByComparingTo("350000");
    }

    @Test
    void 첫날과_마지막_밤의_특별_요금도_반영한다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(3);
        List<RoomSpecialPrice> specialPrices = List.of(
                specialPrice(CHECK_IN, "120000"),
                specialPrice(CHECK_IN.plusDays(2), "80000"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        assertThat(stayPrice.total()).isEqualByComparingTo("300000");
        assertThat(stayPrice.nights())
                .extracting(NightPrice::price)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("120000"), new BigDecimal("100000"), new BigDecimal("80000"));
    }

    @Test
    void 체크아웃_날의_특별_요금은_계산에_포함하지_않는다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(2);
        // 체크아웃 날은 밤이 아니다
        List<RoomSpecialPrice> specialPrices = List.of(specialPrice(checkOut, "999999"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        assertThat(stayPrice.nights()).hasSize(2);
        assertThat(stayPrice.total()).isEqualByComparingTo("200000");
    }

    @Test
    void 체크인_전날의_특별_요금은_계산에_포함하지_않는다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(2);
        List<RoomSpecialPrice> specialPrices = List.of(specialPrice(CHECK_IN.minusDays(1), "999999"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        assertThat(stayPrice.total()).isEqualByComparingTo("200000");
    }

    @Test
    void 하룻밤도_계산한다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(1);
        List<RoomSpecialPrice> specialPrices = List.of(specialPrice(CHECK_IN, "130000"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        assertThat(stayPrice.nights()).hasSize(1);
        assertThat(stayPrice.total()).isEqualByComparingTo("130000");
        assertThat(stayPrice.averagePerNight()).isEqualByComparingTo("130000");
    }

    @Test
    void 평균은_총액을_박수로_나눈_값이다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(3);
        List<RoomSpecialPrice> specialPrices = List.of(specialPrice(CHECK_IN.plusDays(1), "150000"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        // 350000 / 3 = 116666.666...
        assertThat(stayPrice.averagePerNight()).isEqualByComparingTo("116666.6667");
    }

    @Test
    void 원_단위로_바꿀_때는_반올림한다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(3);
        List<RoomSpecialPrice> specialPrices = List.of(specialPrice(CHECK_IN.plusDays(1), "150000"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        assertThat(stayPrice.totalWon()).isEqualTo(350000L);
        // 116666.67을 반올림한다
        assertThat(stayPrice.averageWon()).isEqualTo(116667L);
    }

    @Test
    void 반올림은_5_이상을_올린다() {
        // Arrange
        LocalDate checkOut = CHECK_IN.plusDays(2);
        // (100000 + 100001) / 2 = 100000.5
        List<RoomSpecialPrice> specialPrices = List.of(specialPrice(CHECK_IN.plusDays(1), "100001"));

        // Act
        StayPrice stayPrice = room.calculateStayPrice(CHECK_IN, checkOut, specialPrices);

        // Assert
        assertThat(stayPrice.averageWon()).isEqualTo(100001L);
    }

    private RoomSpecialPrice specialPrice(LocalDate date, String price) {
        return RoomSpecialPrice.builder().room(room).priceDate(date).price(new BigDecimal(price)).build();
    }
}
