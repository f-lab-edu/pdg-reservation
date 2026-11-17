package com.pdg.reservation.review.dto;

import com.pdg.reservation.review.entity.Review;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 리뷰 등록 결과. Facade가 엔티티에서 만들어 Controller에 넘긴다
public record ReviewInfo(
        Long id,
        String nickName,
        BigDecimal rating,
        String content,
        LocalDateTime createdAt) {

    public static ReviewInfo from(Review review) {
        return new ReviewInfo(
                review.getId(),
                review.getNickName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt());
    }
}
