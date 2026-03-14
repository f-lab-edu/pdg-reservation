package com.pdg.reservation.review.repository;

import com.pdg.reservation.review.dto.ReviewSearchCondition;
import com.pdg.reservation.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {

    Page<Review> findMyReviews(Long memberId, ReviewSearchCondition condition, Pageable pageable);
    Page<Review> findAccommodationReviews(Long accommodationId, ReviewSearchCondition condition, Pageable pageable);
}
