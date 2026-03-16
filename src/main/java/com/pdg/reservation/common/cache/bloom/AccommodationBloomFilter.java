package com.pdg.reservation.common.cache.bloom;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccommodationBloomFilter {
    private final RedissonClient redissonClient;
    private RBloomFilter<Long> bloomFilter;

    private static final String FILTER_NAME = "accommodation-exists-filter";
    private static final long EXPECTED_INSERTIONS = 50000L;
    private static final double FALSE_PROBABILITY = 0.03;

    @PostConstruct
    public void init() {
        this.bloomFilter = redissonClient.getBloomFilter(FILTER_NAME);
        this.bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
        log.info("블룸 필터 초기화 완료: {}", FILTER_NAME);
    }

    public boolean couldExist(Long id) {
        return bloomFilter.contains(id);
    }

    public void add(Long id) {
        bloomFilter.add(id);
    }
}