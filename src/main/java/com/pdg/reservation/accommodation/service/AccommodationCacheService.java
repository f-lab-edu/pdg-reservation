package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.common.constant.RedisKeyNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationCacheService {

    private final AccommodationAtomicReader accommodationQueryService;

    /**
     * 숙소 상세 정보 조회
     * cacheNames: RedisConfig에서 설정한 ACCOMMODATION_DETAIL 사용
     * key: 숙소의 ID를 키로 사용 (예: ACCOMMODATION_DETAIL::1)
     */
    @Cacheable(value = RedisKeyNames.ACCOMMODATION_DETAIL, key = "#accommodationId", cacheManager = "cacheManager")
    public AccommodationDetailResponse getAccommodationDetail(Long accommodationId) {
        log.info("[Cache Miss] DB에서 숙소 상세 조회 - 숙소 ID: {}", accommodationId);
        return accommodationQueryService.getAccommodationDetailWithLock(accommodationId);
    }

}
