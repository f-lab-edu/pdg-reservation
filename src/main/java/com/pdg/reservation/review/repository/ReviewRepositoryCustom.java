package com.pdg.reservation.review.repository;

import com.pdg.reservation.review.dto.ReviewCreateRequest;
import com.pdg.reservation.review.dto.ReviewResponse;
import com.pdg.reservation.review.dto.ReviewSearchCondition;
import com.pdg.reservation.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ReviewRepositoryCustom {

    BigDecimal calculateAverageRating(Long accommodationId);
    Page<Review> findMyReviews(Long memberId, ReviewSearchCondition condition, Pageable pageable);
    Page<Review> findAccommodationReviews(Long accommodationId, ReviewSearchCondition condition, Pageable pageable);
}
