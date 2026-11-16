package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.cache.bloom.AccommodationBloomFilter;
import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.room.service.RoomPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationCacheService accommodationCacheService;
    private final AccommodationBloomFilter bloomFilter;
    private final AccommodationCacheGenerationService accommodationCacheGenerationService;
    private final RoomPriceService roomPriceService;

    public Page<AccommodationSearchResponse> search(AccommodationSearchCondition condition, Pageable pageable) {
        Page<AccommodationSearchResponse> page = accommodationRepository.search(condition, pageable);
        fillMinPrices(page, condition);
        return page;
    }

    // 숙소마다 예약 가능한 객실의 1박 평균가 중 최저가를 채운다. 계산은 RoomPriceService 한 곳에서 한다
    private void fillMinPrices(Page<AccommodationSearchResponse> page, AccommodationSearchCondition condition) {
        List<Long> accommodationIds = page.getContent().stream().map(AccommodationSearchResponse::getId).toList();
        Map<Long, Long> minPrices = roomPriceService.minAveragePriceByAccommodation(
                accommodationIds, condition.getCheckInDate(), condition.getCheckOutDate(), condition.getCapacity());
        page.getContent().forEach(accommodation -> accommodation.updateMinPrice(minPrices.get(accommodation.getId())));
    }

    public AccommodationDetailResponse getAccommodationDetail(Long accommodationId) {
        validateExists(accommodationId);

        // 평점이 바뀌면 소비자가 이 세대를 올린다. 세대가 키에 들어가므로 옛 상세가 새 세대의 키에 남지 않는다
        int generation = accommodationCacheGenerationService.getGeneration(accommodationId);
        return accommodationCacheService.getAccommodationDetail(accommodationId, generation);
    }

    public void validateExists(Long accommodationId){
        // 1차로 블룸 필터 확인 (DB 부하 감소)
        if (!bloomFilter.couldExist(accommodationId)) {
            log.warn("[불룸 필터 작동] 존재하지 않는 숙소 ID: {}", accommodationId);
            throw new CustomException(ErrorCode.ACC_NOT_FOUND);
        }

        // 2차로 실제 DB 확인 (정합성 보장)
        if (!accommodationRepository.existsById(accommodationId)) {
            log.warn("[DB 조회] 존재하지 않는 숙소 ID: {}", accommodationId);
            throw new CustomException(ErrorCode.ACC_NOT_FOUND);
        }
    }
}
