package com.pdg.reservation.accommodation.service;

import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.enums.EventType;
import com.pdg.reservation.common.repository.EventLogRepository;
import com.pdg.reservation.common.service.IdempotencyService;
import com.pdg.reservation.review.service.ReviewCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRatingService {

    private final AccommodationRepository accommodationRepository;
    private final EventLogRepository eventLogRepository;
    private final ReviewCacheService reviewCacheService;
    private final IdempotencyService idempotencyService;
    @Transactional
    public void updateAccommodationRating(ReviewKafkaEvent event) {
        // 1. 멱등성 체크
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), event.getActionType(), event.getReviewId())) {
            return;
        }

        // 2. 평점 업데이트 (Atomic Query 권장)
        Long updateCount = 0L;
        if (EventType.REVIEW_CREATED.name().equals(event.getActionType())) {
            updateCount = accommodationRepository.incrementRating(event.getAccommodationId(), event.getRating());
        } else {
            updateCount = accommodationRepository.decrementRating(event.getAccommodationId(), event.getRating());
        }

        // 3. 숙소가 없는 경우 알림 처리
        if(updateCount == 0){
            log.error("평점 변경 실패 - 숙소 없음  (ID: {})", event.getAccommodationId());
            //errorAlertService.sendMessengerAlert(...);
            return;
        }

        // 4. 후처리 (캐시 만료)
        reviewCacheService.increaseVersion(event.getAccommodationId());
        reviewCacheService.evictAccommodationDetail(event.getAccommodationId());

        log.info("[Rating Service] 평점 업데이트 성공: {}", event.getEventId());
    }
}