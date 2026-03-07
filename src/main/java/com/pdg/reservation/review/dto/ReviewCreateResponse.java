package com.pdg.reservation.review.dto;

import com.pdg.reservation.review.entity.Review;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewCreateResponse {

    private final Long id;
    private final String nickName;
    private final BigDecimal rating;
    private final String content;
    private final LocalDateTime createdAt;

    public static ReviewCreateResponse from(Review review) {
        return ReviewCreateResponse.builder()
                .id(review.getId())
                .nickName(review.getNickName())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
