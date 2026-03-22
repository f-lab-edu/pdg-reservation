package com.pdg.reservation.review.event;

import com.pdg.reservation.common.enums.EventType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 리뷰 등록 시, 평점 계산을 하기 위한 객체
 */
@Getter
public class ReviewDeletedEvent {

    private final Long accommodationId;
    private final BigDecimal rating;
    private final Long reviewId;
    private final String eventId; // 멱등성 식별자
    private final LocalDateTime createdAt;
    private final EventType eventType = EventType.REVIEW_CREATED;

    public ReviewDeletedEvent(Long accommodationId, BigDecimal rating,
                              Long reviewId, String eventId, LocalDateTime createdAt) {
        this.accommodationId = accommodationId;
        this.rating = rating;
        this.reviewId = reviewId;
        this.eventId = eventId;
        this.createdAt = createdAt;
    }
}




