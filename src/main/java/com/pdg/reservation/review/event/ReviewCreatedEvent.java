package com.pdg.reservation.review.event;

import com.pdg.reservation.common.enums.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 리뷰 등록 시, 평점 계산을 하기 위한 객체
 */
public class ReviewCreatedEvent {

    public final Long accommodationId;
    public final BigDecimal rating;
    public final Long reviewId;
    public final String eventId; // 멱등성 식별자
    public final LocalDateTime createdAt;
    public final EventType eventType = EventType.REVIEW_CREATED;

    public ReviewCreatedEvent(Long accommodationId, BigDecimal rating,
                              Long reviewId, String eventId, LocalDateTime createdAt) {
        this.accommodationId = accommodationId;
        this.rating = rating;
        this.reviewId = reviewId;
        this.eventId = eventId;
        this.createdAt = createdAt;
    }
}
