package com.pdg.reservation.review.service;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.event.ReservationRollbackEvent;
import com.pdg.reservation.reservation.repository.ReservationRepository;
import com.pdg.reservation.review.dto.*;
import com.pdg.reservation.review.entity.Review;
import com.pdg.reservation.review.event.ReviewCreatedEvent;
import com.pdg.reservation.review.event.ReviewDeletedEvent;
import com.pdg.reservation.review.repository.ReviewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
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
        eventPublisher.publishEvent(new ReviewCreatedEvent(review.getAccommodation().getId()));
        return ReviewCreateResponse.from(review);
    }

    public Page<ReviewResponse> findMyReviews(Long memberId, ReviewSearchCondition condition, Pageable pageable
    ) {
        Page<Review> myReviews = reviewRepository.findMyReviews(memberId, condition, pageable);
        return myReviews.map(review -> ReviewResponse.from(review, false));
    }

    public Page<ReviewResponse> getAccommodationReviews(Long accommodationId, ReviewSearchCondition condition, Pageable pageable
    ) {
        Page<Review> accReviews = reviewRepository.findAccommodationReviews(accommodationId, condition, pageable);
        return accReviews.map(review -> ReviewResponse.from(review, true));
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
        eventPublisher.publishEvent(new ReviewDeletedEvent(review.getAccommodation().getId()));
        return ReviewDeleteResponse.from(reviewId, review.getDeletedAt());
    }

}
