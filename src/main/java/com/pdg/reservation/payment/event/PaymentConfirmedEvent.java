package com.pdg.reservation.payment.event;


import com.pdg.reservation.common.enums.EventType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentConfirmedEvent {

    private final Long memberId;
    private final Long reservationId;
    private final String eventId; // 멱등성 식별자
    private final LocalDateTime createdAt;
    private final EventType eventType = EventType.PAYMENT_CONFIRMED;

    public PaymentConfirmedEvent(Long memberId, Long reservationId, String eventId, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.reservationId = reservationId;
        this.eventId = eventId;
        this.createdAt = createdAt;
    }
}
