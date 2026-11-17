package com.pdg.reservation.reservation.dto;

import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// 예약 생성 결과. Facade가 엔티티에서 만들어 Controller에 넘긴다
public record ReservationInfo(
        UUID reservationId,
        String accommodationName,
        String roomName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int guestCount,
        BigDecimal totalPrice,
        ReservationStatus status,
        String guestName,
        String guestPhoneNumber,
        LocalDateTime createdAt) {

    public static ReservationInfo from(Reservation reservation) {
        return new ReservationInfo(
                reservation.getPublicId(),
                reservation.getAccommodation().getName(),
                reservation.getRoom().getName(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getGuestCount(),
                reservation.getTotalPrice(),
                reservation.getStatus(),
                reservation.getGuestName(),
                reservation.getGuestPhoneNumber(),
                reservation.getCreatedAt());
    }
}
