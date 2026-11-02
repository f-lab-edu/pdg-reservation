package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.cache.bloom.AccommodationBloomFilter;
import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

import static com.pdg.reservation.fixture.AccommodationGenerator.generateAccommodation;

@Component
public class AccommodationFixture {

    private final TestRestTemplate client;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationBloomFilter bloomFilter;

    public AccommodationFixture(ApiClient apiClient,
                                AccommodationRepository accommodationRepository,
                                AccommodationBloomFilter bloomFilter) {
        this.client = apiClient.rest();
        this.accommodationRepository = accommodationRepository;
        this.bloomFilter = bloomFilter;
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }

    // 숙소를 저장하고 블룸 필터에 등록한 뒤 돌려준다
    public Accommodation createAccommodation() {
        Accommodation accommodation = accommodationRepository.save(generateAccommodation());
        bloomFilter.add(accommodation.getId());
        return accommodation;
    }
}
