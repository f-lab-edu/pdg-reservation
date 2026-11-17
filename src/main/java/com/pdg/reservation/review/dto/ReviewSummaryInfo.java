package com.pdg.reservation.review.dto;

import com.pdg.reservation.common.util.CommonUtil;
import com.pdg.reservation.review.entity.Review;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 리뷰 목록의 한 건. 숙소 리뷰 첫 페이지 캐시에 들어가는 값이라 JSON 역직렬화가 되도록 기본 생성자를 둔다
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSummaryInfo {

    private Long id;
    private String nickname;          // 숙소 리뷰에서는 가려서 보여준다(예: 홍*동)
    private BigDecimal rating;
    private String content;
    private String accommodationName;
    private String roomName;
    private LocalDateTime createdAt;

    public static ReviewSummaryInfo from(Review review, boolean isMasked) {
        String nickName = review.getNickName();
        return ReviewSummaryInfo.builder()
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
