package com.pdg.reservation.review.listener;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.review.event.ReviewCreatedEvent;
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
    public void updateAccommodationRating(ReviewCreatedEvent event) {
        log.info("숙소 평점 업데이트 시작 - ID: {}", event.accommodationId);
        Accommodation accommodation = accommodationRepository.findById(event.accommodationId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACC_NOT_FOUND));

        BigDecimal ratingAvg = reviewRepository.calculateAverageRating(event.accommodationId);
        accommodation.updateAverageRating(ratingAvg);
        // 3. 추후 카프카로 이관
    }
}