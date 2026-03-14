package com.pdg.reservation.review.listener;

import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.repository.EventLogRepository;
import com.pdg.reservation.review.event.ReviewCreatedEvent;
import com.pdg.reservation.review.event.ReviewDeletedEvent;
import com.pdg.reservation.review.service.ReviewCacheService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final AccommodationRepository accommodationRepository;
    private final EventLogRepository eventLogRepository;
    private final ReviewCacheService reviewCacheService;
    private final EntityManager em;

    @Async("reviewAsyncExecutor")
    @Transactional(propagation = REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        // 1. 멱등성 체크: 성공 시 1, 중복 시 0 반환 (예외 발생 안 함)
        if(isProcessed(event.eventId, event.eventType.name(), event.reviewId)){
            return;
        }

        Long accommodationId = event.accommodationId;
        BigDecimal rating = event.rating;
        log.info("리뷰 생성 이벤트 수신 - 숙소 ID: {}", accommodationId);

        Long updateCount = accommodationRepository.incrementRating(accommodationId, rating);
        em.flush();
        em.clear();

        if(updateCount == 0){
            log.error("평점 증가 실패 - 숙소 없음  (ID: {})", accommodationId);
            return;
        }

        cacheClear(accommodationId);
        log.info("숙소 평점 증가 업데이트 완료 - ID: {}", accommodationId);
    }

    @Async("reviewAsyncExecutor")
    @Transactional(propagation = REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewDeleted(ReviewDeletedEvent event) {

        if(isProcessed(event.eventId, event.eventType.name(), event.reviewId)){
            return;
        }

        Long accommodationId = event.accommodationId;
        BigDecimal rating = event.rating;
        log.info("리뷰 삭제 이벤트 수신 - 숙소 ID: {}", accommodationId);

        Long updateCount = accommodationRepository.decrementRating(accommodationId, rating);
        em.flush();
        em.clear();

        if(updateCount == 0){
            log.error("평점 감소 실패 - 숙소 없음 또는 카운트 0 (ID: {})", accommodationId);
            return;
        }

        cacheClear(accommodationId);
        log.info("숙소 평점 감소 업데이트 완료 - ID: {}", accommodationId);


        // 추후 카프카 이관
    }


    private boolean isProcessed(String eventId, String type, Long aggregateId) {
        int inserted = eventLogRepository.insertIgnore(eventId, type, aggregateId);
        if (inserted == 0) {
            log.info("[IDEMPOTENCY] 중복 이벤트 스킵: {}", eventId);
            return true;
        }
        return false;
    }

    private void cacheClear(Long accommodationId) {

        // 버전 증가 및 상세 캐시 삭제
        reviewCacheService.increaseVersion(accommodationId);
        reviewCacheService.evictAccommodationDetail(accommodationId);
    }
}