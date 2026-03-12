package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationAtomicReader accommodationQueryService;

    public Page<AccommodationSearchResponse> search(AccommodationSearchCondition condition, Pageable pageable) {
        return accommodationRepository.search(condition, pageable);
    }

    /**
     * 숙소 상세 정보 조회
     * cacheNames: RedisConfig에서 설정한 ACCOMMODATION_DETAIL 사용
     * key: 숙소의 ID를 키로 사용 (예: ACCOMMODATION_DETAIL::1)
     */
    @Cacheable(value = RedisKeyNames.ACCOMMODATION_DETAIL, key = "#accommodationId", cacheManager = "cacheManager", sync = true)
    public AccommodationDetailResponse getAccommodationDetail(Long accommodationId) {
        log.info("[Cache Miss] DB에서 숙소 상세 조회 - 숙소 ID: {}", accommodationId);
        return accommodationQueryService.getAccommodationDetailWithLock(accommodationId);
    }

    public void validateExists(Long accommodationId){
        if (!accommodationRepository.existsById(accommodationId)) {
            throw new CustomException(ErrorCode.ACC_NOT_FOUND);
        }
    }
}
