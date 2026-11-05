package com.pdg.reservation.fixture;

import com.pdg.reservation.review.dto.ReviewCreateRequest;

import java.math.BigDecimal;
import java.util.UUID;

public final class ReviewGenerator {

    private static final BigDecimal DEFAULT_RATING = new BigDecimal("4.0");

    private ReviewGenerator() {
    }

    // 예약 id와 평점을 지정하고 내용은 임의 값으로 채운 올바른 리뷰 요청을 만든다
    public static ReviewCreateRequest generateReviewCreateRequest(UUID reservationId, BigDecimal rating) {
        return validRequestBuilder().reservationId(reservationId).rating(rating).build();
    }

    // 예약 id, 평점, 내용을 모두 지정한 리뷰 요청을 만든다
    public static ReviewCreateRequest generateReviewCreateRequest(UUID reservationId, BigDecimal rating, String content) {
        return validRequestBuilder().reservationId(reservationId).rating(rating).content(content).build();
    }

    // 예약 id만 지정하고 나머지는 올바른 임의 값으로 채운 리뷰 요청을 만든다
    public static ReviewCreateRequest generateReviewCreateRequestWithReservationId(UUID reservationId) {
        return validRequestBuilder().reservationId(reservationId).build();
    }

    public static ReviewCreateRequest generateReviewCreateRequestWithRating(BigDecimal rating) {
        return validRequestBuilder().rating(rating).build();
    }

    public static ReviewCreateRequest generateReviewCreateRequestWithContent(String content) {
        return validRequestBuilder().content(content).build();
    }

    // 지정한 길이의 리뷰 내용을 만든다
    public static String generateContentOfLength(int length) {
        return "가".repeat(length);
    }

    private static ReviewCreateRequest.ReviewCreateRequestBuilder validRequestBuilder() {
        return ReviewCreateRequest.builder()
                .reservationId(UUID.randomUUID())
                .rating(DEFAULT_RATING)
                .content("리뷰-" + UUID.randomUUID());
    }
}
