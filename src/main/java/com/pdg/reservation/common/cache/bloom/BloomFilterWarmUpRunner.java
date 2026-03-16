package com.pdg.reservation.common.cache.bloom;

import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterWarmUpRunner implements CommandLineRunner {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationBloomFilter accommodationBloomFilter;

    @Override
    public void run(String... args) {
        log.info("블룸 필터 Warm-up 시작...");
        List<Long> allIds = accommodationRepository.findAllIds();
        allIds.forEach(accommodationBloomFilter::add);
        log.info("블룸 필터 Warm-up 완료: {}건 적재", allIds.size());
    }
}