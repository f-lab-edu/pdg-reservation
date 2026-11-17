package com.pdg.reservation.review.facade;

import com.pdg.reservation.accommodation.service.AccommodationCacheGenerationService;
import com.pdg.reservation.accommodation.service.AccommodationService;
import com.pdg.reservation.common.dto.PageInfo;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.service.ReservationService;
import com.pdg.reservation.review.dto.ReviewCreateRequest;
import com.pdg.reservation.review.dto.ReviewDeleteInfo;
import com.pdg.reservation.review.dto.ReviewInfo;
import com.pdg.reservation.review.dto.ReviewSummaryInfo;
import com.pdg.reservation.review.dto.ReviewSearchCondition;
import com.pdg.reservation.review.entity.Review;
import com.pdg.reservation.review.service.ReviewCacheService;
import com.pdg.reservation.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewFacade {

    private final ReviewService reviewService;
    private final ReviewCacheService reviewCacheService;
    private final ReservationService reservationService;
    private final AccommodationService accommodationService;
    private final AccommodationCacheGenerationService accommodationCacheGenerationService;

    @Transactional
    public ReviewInfo createReview(Member member, ReviewCreateRequest request) {
        Reservation reservation = reservationService.getByPublicIdAndMemberId(request.getReservationId(), member.getId());
        Review review = reviewService.createReview(member, reservation, request);
        return ReviewInfo.from(review);
    }

    public PageInfo<ReviewSummaryInfo> findMyReviews(Long memberId, ReviewSearchCondition condition, Pageable pageable) {
        return PageInfo.from(reviewService.findMyReviews(memberId, condition, pageable)
                .map(review -> ReviewSummaryInfo.from(review, false)));
    }

    public PageInfo<ReviewSummaryInfo> getAccommodationReviews(
            Long accommodationId, ReviewSearchCondition condition, Pageable pageable) {
        accommodationService.validateExists(accommodationId);

        int generation = accommodationCacheGenerationService.getGeneration(accommodationId);
        return reviewCacheService.getReviewsWithCache(accommodationId, generation, condition, pageable);
    }

    @Transactional
    public ReviewDeleteInfo deleteReview(Long memberId, Long reviewId) {
        Review review = reviewService.deleteReview(memberId, reviewId);
        return new ReviewDeleteInfo(reviewId, review.getDeletedAt());
    }

}
