package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.pdg.reservation.accommodation.repository.AccommodationRepository;
import com.pdg.reservation.common.cache.bloom.AccommodationBloomFilter;
import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static com.pdg.reservation.fixture.AccommodationGenerator.generateAccommodation;
import static com.pdg.reservation.fixture.AccommodationGenerator.generateCity;
import static com.pdg.reservation.fixture.AccommodationGenerator.generateNonOperationalAccommodation;
import static com.pdg.reservation.fixture.AccommodationGenerator.generateAccommodationWithImages;

@Component
public class AccommodationFixture {

    private final TestRestTemplate client;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationBloomFilter bloomFilter;
    private final RoomFixture roomFixture;

    public AccommodationFixture(ApiClient apiClient,
                                AccommodationRepository accommodationRepository,
                                AccommodationBloomFilter bloomFilter,
                                RoomFixture roomFixture) {
        this.client = apiClient.rest();
        this.accommodationRepository = accommodationRepository;
        this.bloomFilter = bloomFilter;
        this.roomFixture = roomFixture;
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

    // 도시와 유형을 지정한 숙소를 저장하고 블룸 필터에 등록한 뒤 돌려준다
    public Accommodation createAccommodation(String city, AccommodationType type) {
        Accommodation accommodation = accommodationRepository.save(generateAccommodation(city, type));
        bloomFilter.add(accommodation.getId());
        return accommodation;
    }

    // 운영 중이 아닌 숙소를 저장하고 블룸 필터에 등록한 뒤 돌려준다
    public Accommodation createNonOperationalAccommodation() {
        Accommodation accommodation = accommodationRepository.save(generateNonOperationalAccommodation());
        bloomFilter.add(accommodation.getId());
        return accommodation;
    }

    // 같은 임의 도시, 호텔 유형의 숙소를 개수만큼 만들고, 숙소마다 체크인~체크아웃 전날까지 재고가 있는 객실을 하나씩 붙인다. 만든 순서대로 돌려준다
    public List<Accommodation> createAccommodationsWithInventories(int count, LocalDate checkInDate, LocalDate checkOutDate) {
        String city = generateCity();
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    Accommodation accommodation = createAccommodation(city, AccommodationType.HOTEL);
                    roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
                    return accommodation;
                })
                .toList();
    }

    // 이미지를 가진 숙소를 저장하고 블룸 필터에 등록한 뒤 돌려준다
    public Accommodation createAccommodationWithImages() {
        Accommodation accommodation = accommodationRepository.save(generateAccommodationWithImages());
        bloomFilter.add(accommodation.getId());
        return accommodation;
    }

    // 숙소를 DB에만 저장하고 블룸 필터에는 등록하지 않는다
    public Accommodation createAccommodationOutsideBloomFilter() {
        return accommodationRepository.save(generateAccommodation());
    }
}
