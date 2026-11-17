package com.pdg.reservation.review.service;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.dto.PageInfo;
import com.pdg.reservation.review.dto.ReviewSummaryInfo;
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

    @DistributedLock(key = "'review:' + #accommodationId + ':' + #generation", throwExceptionOnFailure = false)
    public PageInfo<ReviewSummaryInfo> getReviewsWithLock(
            Long accommodationId, int generation, ReviewSearchCondition condition, Pageable pageable) {

        // [Double-Check] 락 획득 후 캐시 재확인
        String cacheKey = generateCacheKey(accommodationId, generation, condition, pageable);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return (PageInfo<ReviewSummaryInfo>) cached;

        return getReviews(accommodationId, generation, condition, pageable);
    }
    public PageInfo<ReviewSummaryInfo> getReviewsWithNoLock(
            Long accommodationId, int generation, ReviewSearchCondition condition, Pageable pageable) {
        return getReviews(accommodationId, generation, condition, pageable);
    }

    public PageInfo<ReviewSummaryInfo> getReviews(Long accommodationId, int generation, ReviewSearchCondition condition, Pageable pageable){
        Page<Review> accReviews = reviewRepository.findAccommodationReviews(accommodationId, condition, pageable);
        return PageInfo.from(accReviews.map(review -> ReviewSummaryInfo.from(review, true)));
    }

    /**
     * @Cacheable의 key SpEL과 동일한 문자열을 생성합니다.
     * SpEL: "'acc:' + #accommodationId + ':gen:' + #generation + ':' + #condition.toCacheKey() + ':sz:' + #pageable.pageSize"
     */
    private String generateCacheKey(Long accommodationId, int generation, ReviewSearchCondition condition, Pageable pageable) {
        return RedisKeyNames.REVIEW_FIRST_PAGE + "::" +
                String.format("acc:%d:gen:%d:%s:sz:%d",
                accommodationId,
                generation,
                condition.toCacheKey(),
                pageable.getPageSize()
        );
    }

}