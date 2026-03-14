package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccommodationAtomicReader {
    private final AccommodationRepository accommodationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @DistributedLock(key = "'acc:' + #id", throwExceptionOnFailure = false)
    public AccommodationDetailResponse getAccommodationDetailWithLock(Long id) {

        // [Double-Check] 락 획득 성공 후, 다시 한번 캐시 확인, 락을 획득하는 사이에 누군가 DB를 갔다와 캐시를 채웠을 수 있음
        String cacheKey = RedisKeyNames.ACCOMMODATION_DETAIL + "::" + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return (AccommodationDetailResponse) cached;

        // 실제 DB 조회
        return accommodationRepository.findById(id)
                .map(AccommodationDetailResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.ACC_NOT_FOUND));
    }
}