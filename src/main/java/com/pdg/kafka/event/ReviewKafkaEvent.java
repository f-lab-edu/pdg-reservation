package com.pdg.kafka.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ReviewKafkaEvent {
    private String eventId;
    private Long accommodationId;
    private Long reviewId;
    private BigDecimal rating;
    private String actionType; // "REVIEW_CREATED" or "REVIEW_DELETED"
    private LocalDateTime createdAt;

    public ReviewKafkaEvent(String eventId, Long accommodationId, Long reviewId,
                            BigDecimal rating, String actionType, LocalDateTime createdAt) {
        this.eventId = eventId;
        this.accommodationId = accommodationId;
        this.reviewId = reviewId;
        this.rating = rating;
        this.actionType = actionType;
        this.createdAt = createdAt;
    }
}