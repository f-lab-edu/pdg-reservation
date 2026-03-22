package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.dto.AccommodationSearchCondition;
import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.cache.bloom.AccommodationBloomFilter;
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
    private final AccommodationCacheService accommodationCacheService;
    private final AccommodationBloomFilter bloomFilter;

    public Page<AccommodationSearchResponse> search(AccommodationSearchCondition condition, Pageable pageable) {
        return accommodationRepository.search(condition, pageable);
    }

    public AccommodationDetailResponse getAccommodationDetail(Long accommodationId) {
        validateExists(accommodationId);

        return accommodationCacheService.getAccommodationDetail(accommodationId);
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
