package com.pdg.reservation.review.service;

import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.review.dto.ReviewResponse;
import com.pdg.reservation.review.dto.ReviewSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewCacheService {

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
            // 결과 예시: review:firstpage::acc:1:gen:3:r:0:s:LATEST:sz:10
            key = "'acc:' + #accommodationId + ':gen:' + #generation + ':' + #condition.toCacheKey() + ':sz:' + #pageable.pageSize",
            condition = "#pageable.pageNumber == 0"
    )
    public PageResponse<ReviewResponse> getReviewsWithCache(
            Long accommodationId, int generation, ReviewSearchCondition condition, Pageable pageable) {

        if(pageable.getPageNumber() == 0){
            log.info("[Cache Miss] DB에서 리뷰 조회 - 숙소 ID: {}, 세대: {}", accommodationId, generation);
            return reviewAtomicReader.getReviewsWithLock(accommodationId, generation, condition, pageable);
        }

        return reviewAtomicReader.getReviewsWithNoLock(accommodationId, generation, condition, pageable);
    }

}