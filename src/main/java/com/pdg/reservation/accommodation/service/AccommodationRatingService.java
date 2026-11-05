package com.pdg.reservation.accommodation.service;

import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.enums.EventType;
import com.pdg.reservation.common.repository.EventLogRepository;
import com.pdg.reservation.common.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccommodationRatingService {

    private final AccommodationRepository accommodationRepository;
    private final EventLogRepository eventLogRepository;
    private final AccommodationCacheGenerationService accommodationCacheGenerationService;
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

        // 4. 후처리 (캐시 만료): 세대를 올리면 리뷰 목록과 숙소 상세 캐시가 모두 새 키로 바뀐다
        // 세대는 커밋이 끝난 뒤에 올린다. 커밋 전에 올리면 그 사이 들어온 조회가 아직 보이지 않는 옛 평점을 읽어 새 세대 키에 캐시한다 (ADR 0011)
        increaseGenerationAfterCommit(event.getAccommodationId());

        log.info("[Rating Service] 평점 업데이트 성공: {}", event.getEventId());
    }

    private void increaseGenerationAfterCommit(Long accommodationId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    accommodationCacheGenerationService.increaseGeneration(accommodationId);
                } catch (Exception e) {
                    // 이미 커밋된 뒤라 되돌릴 수 없다. 캐시는 TTL이 지나면 갱신된다
                    log.error("커밋 뒤 캐시 세대 증가 실패 - 숙소 ID: {}", accommodationId, e);
                }
            }
        });
    }
}