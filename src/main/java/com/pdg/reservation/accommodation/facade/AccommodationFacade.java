package com.pdg.reservation.accommodation.facade;

import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.accommodation.dto.AccommodationDetailInfo;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchInfo;
import com.pdg.reservation.common.dto.PageInfo;
import com.pdg.reservation.accommodation.service.AccommodationCacheGenerationService;
import com.pdg.reservation.accommodation.service.AccommodationCacheService;
import com.pdg.reservation.accommodation.service.AccommodationRatingService;
import com.pdg.reservation.accommodation.service.AccommodationService;
import com.pdg.reservation.common.service.IdempotencyService;
import com.pdg.reservation.room.service.RoomPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationFacade {

    private final AccommodationService accommodationService;
    private final AccommodationCacheService accommodationCacheService;
    private final AccommodationCacheGenerationService accommodationCacheGenerationService;
    private final AccommodationRatingService accommodationRatingService;
    private final RoomPriceService roomPriceService;
    private final IdempotencyService idempotencyService;

    public PageInfo<AccommodationSearchInfo> search(AccommodationSearchCondition condition, Pageable pageable) {
        Page<AccommodationSearchInfo> page = accommodationService.search(condition, pageable);
        return PageInfo.from(fillMinPrices(page, condition));
    }

    // 숙소마다 예약 가능한 객실의 1박 평균가 중 최저가를 채운다. 계산은 RoomPriceService 한 곳에서 한다
    private Page<AccommodationSearchInfo> fillMinPrices(Page<AccommodationSearchInfo> page, AccommodationSearchCondition condition) {
        List<Long> accommodationIds = page.getContent().stream().map(AccommodationSearchInfo::id).toList();
        Map<Long, Long> minPrices = roomPriceService.minAveragePriceByAccommodation(
                accommodationIds, condition.getCheckInDate(), condition.getCheckOutDate(), condition.getCapacity());
        return page.map(accommodation -> accommodation.withMinPrice(minPrices.get(accommodation.id())));
    }

    public AccommodationDetailInfo getAccommodationDetail(Long accommodationId) {
        accommodationService.validateExists(accommodationId);

        // 평점이 바뀌면 소비자가 이 세대를 올린다. 세대가 키에 들어가므로 옛 상세가 새 세대의 키에 남지 않는다
        int generation = accommodationCacheGenerationService.getGeneration(accommodationId);
        return accommodationCacheService.getAccommodationDetail(accommodationId, generation);
    }

    // 리뷰 이벤트를 받아 평점을 갱신한다(Kafka 컨슈머가 호출)
    @Transactional
    public void updateRating(ReviewKafkaEvent event) {
        if (idempotencyService.isAlreadyProcessed(event.getEventId(), event.getActionType(), event.getReviewId())) {
            return;
        }
        boolean updated = accommodationRatingService.updateRating(event);
        if (updated) {
            increaseGenerationAfterCommit(event.getAccommodationId());
            log.info("[Rating] 평점 업데이트 성공: {}", event.getEventId());
        }
    }

    // 세대는 커밋이 끝난 뒤에 올린다. 커밋 전에 올리면 그 사이 들어온 조회가 아직 보이지 않는 옛 평점을 읽어 새 세대 키에 캐시한다 (ADR 0011)
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
