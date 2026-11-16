package com.pdg.reservation.room.price;

import java.math.BigDecimal;
import java.time.LocalDate;

// 하룻밤의 날짜와 가격(등급 할인 전)
public record NightPrice(LocalDate date, BigDecimal price) {
}
