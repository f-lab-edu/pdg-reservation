package com.pdg.reservation.review.listener;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.review.event.ReviewCreatedEvent;
import com.pdg.reservation.review.event.ReviewDeletedEvent;
import com.pdg.reservation.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.Optional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final AccommodationRepository accommodationRepository;
    private final ReviewRepository reviewRepository;

    @Async("reviewAsyncExecutor")
    @Transactional(propagation = REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        log.info("리뷰 생성 이벤트 수신 - 숙소 ID: {}", event.accommodationId);
        updateRating(event.accommodationId);
    }

    @Async("reviewAsyncExecutor")
    @Transactional(propagation = REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewDeleted(ReviewDeletedEvent event) {
        log.info("리뷰 삭제 이벤트 수신 - 숙소 ID: {}", event.accommodationId);
        updateRating(event.accommodationId);
    }

    private void updateRating(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACC_NOT_FOUND));

        BigDecimal ratingAvg = reviewRepository.calculateAverageRating(accommodationId);
        accommodation.updateAverageRating(ratingAvg);

        log.info("숙소 평점 업데이트 완료 - ID: {}, 새 평점: {}", accommodationId, ratingAvg);

        // 추후 카프카 이관
    }
}