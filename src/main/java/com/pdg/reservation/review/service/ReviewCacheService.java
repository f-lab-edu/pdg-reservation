package com.pdg.reservation.review.service;

import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.review.dto.ReviewResponse;
import com.pdg.reservation.review.dto.ReviewSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ReviewAtomicReader reviewAtomicReader;

    /**
     * 숙소별 리뷰 조회 (첫페이지만 캐싱)
     * value: RedisConfig에 설정한 "reviewFirstPage" (TTL 10분)
     * key: 숙소 ID를 기준으로 식별
     * condition: 페이지 번호가 0일 때만 캐시 적용
     */
    // 1. 실제 캐싱 로직
    @Cacheable(
            value = RedisKeyNames.REVIEW_FIRST_PAGE,
            // 모든 항목에 접두어(acc, v, sz)를 붙여 가독성 향상
            // 결과 예시: review:firstpage::acc:1:v:3:r:0:s:LATEST:sz:10
            key = "'acc:' + #accommodationId + ':v:' + #version + ':' + #condition.toCacheKey() + ':sz:' + #pageable.pageSize",
            condition = "#pageable.pageNumber == 0",
            sync = true // Cache Stampede 방지 보조, 단일 서버 부하 감소
    )
    public PageResponse<ReviewResponse> getReviewsWithCache(
            Long accommodationId, int version, ReviewSearchCondition condition, Pageable pageable) {

        if(pageable.getPageNumber() == 0){
            log.info("[Cache Miss] DB에서 리뷰 조회 - 숙소 ID: {}, 버전: {}", accommodationId, version);
            return reviewAtomicReader.getReviewsWithLock(accommodationId, version, condition, pageable);
        }

        return reviewAtomicReader.getReviewsWithNoLock(accommodationId, version, condition, pageable);
    }

    // 2. 버전 조회 (setIfAbsent로 원자적 처리)
    public int getVersion(Long accommodationId) {
        String key = RedisKeyNames.REVIEW_VERSION_PREFIX + accommodationId;
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

    // 3. 버전 증가
    public void increaseVersion(Long accommodationId) {
        redisTemplate.opsForValue().increment(RedisKeyNames.REVIEW_VERSION_PREFIX + accommodationId);
    }

    // 4. 숙소 상세 캐시 삭제
    @CacheEvict(value = RedisKeyNames.ACCOMMODATION_DETAIL, key = "#accommodationId")
    public void evictAccommodationDetail(Long accommodationId) {
        log.info("[Cache Evict] 숙소 상세 캐시 제거");
    }
}