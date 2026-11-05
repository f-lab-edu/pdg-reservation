package com.pdg.reservation.accommodation.service;

import com.pdg.reservation.common.constant.RedisKeyNames;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 숙소별 캐시 버전. 리뷰 목록과 숙소 상세 캐시가 함께 쓴다.
 * 캐시 키에 버전을 넣고, 값이 바뀌면 버전을 올려 새 키로 바꾼다. 지우는 방식에서 생기는
 * "지운 뒤 옛 값이 다시 들어오는" 경쟁을 피한다. (ADR 0011)
 */
@Service
@RequiredArgsConstructor
public class AccommodationCacheVersionService {

    private final StringRedisTemplate redisTemplate;

    // 버전 조회 (setIfAbsent로 원자적 처리)
    public int getVersion(Long accommodationId) {
        String key = RedisKeyNames.ACCOMMODATION_VERSION_PREFIX + accommodationId;
        String version = redisTemplate.opsForValue().get(key);

        if (version == null) {
            // 1. 일단 내가 1로 초기화를 시도해봄 (SETNX)
            redisTemplate.opsForValue().setIfAbsent(key, "1");

            // 2. [핵심] 성공하든 실패하든, 그 '직후'에 다시 읽어옴
            // 그 사이에 누군가 INCR(버전업)을 했을 수도 있기 때문!
            version = redisTemplate.opsForValue().get(key);
        }

        // 만약 여전히 null이라면 (Redis 일시 장애 등) 기본값 1 반환
        return (version == null) ? 1 : Integer.parseInt(version);
    }

    // 버전 증가
    public void increaseVersion(Long accommodationId) {
        redisTemplate.opsForValue().increment(RedisKeyNames.ACCOMMODATION_VERSION_PREFIX + accommodationId);
    }
}
