package com.pdg.reservation.review.dto;

import com.pdg.reservation.common.util.CommonUtil;
import com.pdg.reservation.review.entity.Review;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewResponse {
    private Long id;
    private String nickname;          // 숙소 상세 조회 시 필요 (예: 홍*동)
    private BigDecimal rating;        // 평점
    private String content;           // 리뷰 내용
    private String accommodationName; // 숙소 정보
    private String roomName;          // 방정보
    private LocalDateTime createdAt;  // 작성일

    public static ReviewResponse from(ReviewSummaryInfo info) {
        return ReviewResponse.builder()
                .id(info.getId())
                .nickname(info.getNickname())
                .content(info.getContent())
                .rating(info.getRating())
                .accommodationName(info.getAccommodationName())
                .roomName(info.getRoomName())
                .createdAt(info.getCreatedAt())
                .build();
    }
}