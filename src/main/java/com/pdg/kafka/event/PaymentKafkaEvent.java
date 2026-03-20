package com.pdg.kafka.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
//@NoArgsConstructor
public class PaymentKafkaEvent {

    private Long memberId;
    private Long reservationId;
    private String eventId;
    private String actionType; // "PAYMENT_CONFIRM"
    private LocalDateTime createdAt;

    public PaymentKafkaEvent(Long memberId, Long reservationId, String eventId,
                             String actionType, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.reservationId = reservationId;
        this.eventId = eventId;
        this.actionType = actionType;
        this.createdAt = createdAt;
    }
}