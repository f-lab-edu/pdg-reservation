package com.pdg.reservation.reservation.dto;

import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationResponse {

    private final UUID reservationId;

    // 숙소 및 객실 정보 (사용자 확인용)
    private final String accommodationName;
    private final String roomName;

    // 예약 일정 및 인원
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final int guestCount;

    // 결제 관련
    private final BigDecimal totalPrice;
    private final ReservationStatus status;

    // 예약자 정보
    private final String guestName;
    private final String guestPhoneNumber;

    // 시간 정보
    private final LocalDateTime createdAt; // 예약 요청 시간

    public static ReservationResponse from(ReservationInfo info) {
        return ReservationResponse.builder()
                .reservationId(info.reservationId())
                .accommodationName(info.accommodationName())
                .roomName(info.roomName())
                .checkInDate(info.checkInDate())
                .checkOutDate(info.checkOutDate())
                .guestCount(info.guestCount())
                .totalPrice(info.totalPrice())
                .status(info.status())
                .guestName(info.guestName())
                .guestPhoneNumber(info.guestPhoneNumber())
                .createdAt(info.createdAt())
                .build();
    }
}