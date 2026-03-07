package com.pdg.reservation.review.dto;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewDeleteResponse {
    private final Long deleteReviewId;
    private final LocalDateTime deletedAt;

    public static ReviewDeleteResponse from(Long reviewId, LocalDateTime deletedAt) {
        return ReviewDeleteResponse.builder()
                .deleteReviewId(reviewId)
                .deletedAt(deletedAt)
                .build();
    }
}
