package com.pdg.reservation.review.dto;

import java.time.LocalDateTime;

// 리뷰 삭제 결과
public record ReviewDeleteInfo(Long reviewId, LocalDateTime deletedAt) {
}
