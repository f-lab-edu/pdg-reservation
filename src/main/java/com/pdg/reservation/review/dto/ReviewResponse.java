package com.pdg.reservation.review.dto;

import com.pdg.reservation.common.util.CommonUtil;
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
public class ReviewResponse {
    private final Long id;
    private final String nickname;          // 숙소 상세 조회 시 필요 (예: 홍*동)
    private final BigDecimal rating;        // 평점
    private final String content;           // 리뷰 내용
    private final String accommodationName; // 숙소 정보
    private final String roomName;          // 방정보
    private final LocalDateTime createdAt;  // 작성일

    public static ReviewResponse from(Review review, boolean isMasked) {
        String nickName = review.getNickName();

        return ReviewResponse.builder()
                .id(review.getId())
                .nickname(isMasked ? CommonUtil.maskNickname(nickName) : nickName)
                .content(review.getContent())
                .rating(review.getRating())
                .accommodationName(review.getAccommodation().getName())
                .roomName(review.getReservation().getRoom().getName())
                .createdAt(review.getCreatedAt())
                .build();
    }
}