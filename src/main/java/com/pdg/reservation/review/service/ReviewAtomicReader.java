package com.pdg.reservation.review.service;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.review.dto.ReviewResponse;
import com.pdg.reservation.review.dto.ReviewSearchCondition;
import com.pdg.reservation.review.entity.Review;
import com.pdg.reservation.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewAtomicReader {
    private final ReviewRepository reviewRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @DistributedLock(key = "'review:' + #accommodationId + ':' + #version", throwExceptionOnFailure = false)
    public PageResponse<ReviewResponse> getReviewsWithLock(
            Long accommodationId, int version, ReviewSearchCondition condition, Pageable pageable) {

        // [Double-Check] 락 획득 후 캐시 재확인
        String cacheKey = generateCacheKey(accommodationId, version, condition, pageable);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return (PageResponse<ReviewResponse>) cached;

        return getReviews(accommodationId, version, condition, pageable);
    }
    public PageResponse<ReviewResponse> getReviewsWithNoLock(
            Long accommodationId, int version, ReviewSearchCondition condition, Pageable pageable) {
        return getReviews(accommodationId, version, condition, pageable);
    }

    public PageResponse<ReviewResponse> getReviews(Long accommodationId, int version, ReviewSearchCondition condition, Pageable pageable){
        Page<Review> accReviews = reviewRepository.findAccommodationReviews(accommodationId, condition, pageable);
        return PageResponse.from(accReviews.map(review -> ReviewResponse.from(review, true)));
    }

    /**
     * @Cacheable의 key SpEL과 동일한 문자열을 생성합니다.
     * SpEL: "'acc:' + #accommodationId + ':v:' + #version + ':' + #condition.toCacheKey() + ':sz:' + #pageable.pageSize"
     */
    private String generateCacheKey(Long accommodationId, int version, ReviewSearchCondition condition, Pageable pageable) {
        return RedisKeyNames.REVIEW_FIRST_PAGE + "::" +
                String.format("acc:%d:v:%d:%s:sz:%d",
                accommodationId,
                version,
                condition.toCacheKey(),
                pageable.getPageSize()
        );
    }

}