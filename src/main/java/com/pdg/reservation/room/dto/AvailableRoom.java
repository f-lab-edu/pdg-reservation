package com.pdg.reservation.room.dto;

import java.math.BigDecimal;

// 조건(기간, 인원)에 예약 가능한 객실. 숙소 검색의 최저가 계산에 쓴다
public record AvailableRoom(Long roomId, Long accommodationId, BigDecimal basePrice) {
}
