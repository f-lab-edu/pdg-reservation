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
     * key: 숙소의 ID와 버전 (예: ACCOMMODATION_DETAIL::1:v:3)
     * 평점이 바뀌면 버전이 오르고 새 키로 조회한다. 지우는 방식에서 생기던 "지운 뒤 옛 값이 다시 들어오는" 경쟁을 피한다. (ADR 0011)
     */
    @Cacheable(value = RedisKeyNames.ACCOMMODATION_DETAIL, key = "#accommodationId + ':v:' + #version", cacheManager = "cacheManager")
    public AccommodationDetailResponse getAccommodationDetail(Long accommodationId, int version) {
        log.info("[Cache Miss] DB에서 숙소 상세 조회 - 숙소 ID: {}, 버전: {}", accommodationId, version);
        return accommodationQueryService.getAccommodationDetailWithLock(accommodationId, version);
    }

}
