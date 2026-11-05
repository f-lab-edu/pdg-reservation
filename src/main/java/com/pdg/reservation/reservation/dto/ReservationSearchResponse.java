package com.pdg.reservation.reservation.dto;

import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ReservationSearchResponse {
    private final UUID id;
    private final String accommodationName;
    private final String roomName;
    private final String mainImage;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final String guestName;
    private final String guestPhoneNumber;
    private final ReservationStatus status;
    private final BigDecimal totalPrice;
    private final LocalDateTime createdAt;
    private final boolean reviewable;        // 리뷰 작성 가능 여부
    private final boolean cancellable;       // 취소(환불) 가능 여부

    @Builder
    @QueryProjection
    public ReservationSearchResponse(UUID id, String accommodationName, String roomName,
                                     String mainImage, LocalDate checkInDate, LocalDate checkOutDate,
                                     String guestName, String guestPhoneNumber,
                                     ReservationStatus status, BigDecimal totalPrice, LocalDateTime createdAt,
                                     Boolean reviewable, Boolean cancellable
    ) {
        this.id =  id;
        this.accommodationName = accommodationName;
        this.roomName = roomName;
        this.mainImage = mainImage;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestName = guestName;
        this.guestPhoneNumber = guestPhoneNumber;
        this.status = status;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
        this.reviewable = reviewable;
        this.cancellable = cancellable;
    }
}