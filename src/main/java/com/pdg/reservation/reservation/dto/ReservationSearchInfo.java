package com.pdg.reservation.reservation.dto;

import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.querydsl.core.annotations.QueryProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// 예약 검색 한 건. Repository가 쿼리에서 바로 만든다
public record ReservationSearchInfo(
        UUID id,
        String accommodationName,
        String roomName,
        String mainImage,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String guestName,
        String guestPhoneNumber,
        ReservationStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        Boolean reviewable,   // 리뷰 작성 가능 여부
        Boolean cancellable   // 취소(환불) 가능 여부
) {

    @QueryProjection
    public ReservationSearchInfo {
    }
}
