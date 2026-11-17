package com.pdg.reservation.review.service;

import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.common.enums.EventType;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.common.outbox.OutboxService;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.review.dto.ReviewCreateRequest;
import com.pdg.reservation.review.dto.ReviewSearchCondition;
import com.pdg.reservation.review.entity.Review;
import com.pdg.reservation.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String REVIEW_TOPIC = "review-topic";

    private final ReviewRepository reviewRepository;
    private final OutboxService outboxService;

    @Transactional
    public Review createReview(Member member, Reservation reservation, ReviewCreateRequest request) {
        reservation.validateReviewEligibility();

        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.create(member, reservation, request.getRating(), request.getContent());
        reviewRepository.save(review);

        recordRatingEvent(EventType.REVIEW_CREATED, review);
        return review;
    }

    public Page<Review> findMyReviews(Long memberId, ReviewSearchCondition condition, Pageable pageable) {
        return reviewRepository.findMyReviews(memberId, condition, pageable);
    }

    @Transactional
    public Review deleteReview(Long memberId, Long reviewId) {
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

        recordRatingEvent(EventType.REVIEW_DELETED, review);
        return review;
    }

    // 숙소 평점을 바꿔야 하는 이벤트는 유실되면 평점이 영영 어긋나므로 Outbox에 같은 트랜잭션으로 기록한다.
    // 숙소 id를 키로 보내 같은 숙소의 등록과 삭제가 같은 파티션에서 순서대로 처리되게 한다
    private void recordRatingEvent(EventType eventType, Review review) {
        Long accommodationId = review.getAccommodation().getId();
        String eventId = UUID.randomUUID().toString();
        outboxService.record(
                eventId,
                eventType,
                REVIEW_TOPIC,
                String.valueOf(accommodationId),
                new ReviewKafkaEvent(eventId, accommodationId, review.getId(), review.getRating(),
                        eventType.name(), LocalDateTime.now()));
    }
}
