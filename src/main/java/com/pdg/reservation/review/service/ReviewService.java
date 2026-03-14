package com.pdg.reservation.review.service;

import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.review.dto.*;
import com.pdg.reservation.review.entity.Review;
import com.pdg.reservation.review.event.ReviewCreatedEvent;
import com.pdg.reservation.review.event.ReviewDeletedEvent;
import com.pdg.reservation.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewCacheService reviewCacheService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewCreateResponse createReview(Member member, ReviewCreateRequest reviewCreateRequest) {
        Reservation reservation = reservationRepository.findByIdAndMemberId(reviewCreateRequest.getReservationId(), member.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVE_NOT_FOUND));

        reservation.validateReviewEligibility();

        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = reviewCreateRequest.toEntity(member, reservation);
        reviewRepository.save(review);

        eventPublisher.publishEvent(
                new ReviewCreatedEvent(
                        review.getAccommodation().getId(),
                        review.getRating(),
                        review.getId(),
                        UUID.randomUUID().toString(),
                        LocalDateTime.now()
                ));

        return ReviewCreateResponse.from(review);
    }

    public Page<ReviewResponse> findMyReviews(Long memberId, ReviewSearchCondition condition, Pageable pageable
    ) {
        Page<Review> myReviews = reviewRepository.findMyReviews(memberId, condition, pageable);
        return myReviews.map(review -> ReviewResponse.from(review, false));
    }

    public PageResponse<ReviewResponse> getAccommodationReviews(
            Long accommodationId, ReviewSearchCondition condition, Pageable pageable) {
        // 1. 버전 조회
        int version = reviewCacheService.getVersion(accommodationId);
        // 2. 캐시 서비스 호출
        return reviewCacheService.getReviewsWithCache(accommodationId, version, condition, pageable);
    }

    @Transactional
    public ReviewDeleteResponse deleteReview(Long memberId, Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (review.isDeleted()) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_DELETED);
        }

        if (!review.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.REVIEW_OWNER_MISMATCH);
        }
        review.delete();
        reviewRepository.save(review);

        eventPublisher.publishEvent(new ReviewDeletedEvent(
                review.getAccommodation().getId(),
                review.getRating(),
                review.getId(),
                UUID.randomUUID().toString(),
                LocalDateTime.now()
        ));
        return ReviewDeleteResponse.from(reviewId, review.getDeletedAt());
    }

}
