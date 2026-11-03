package com.pdg.reservation.api.v1.accommodations.id;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.dto.ImageResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.AccommodationImage;
import com.pdg.reservation.accommodation.service.AccommodationAtomicReader;
import com.pdg.reservation.common.constant.RedisKeyNames;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/accommodations/{id}")
@IntegrationTest
@Import(TestContainersConfig.class)
class GET_specs {

    @Autowired
    AccommodationFixture accommodationFixture;

    @Autowired
    ApiClient apiClient;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @Autowired
    RedisCleanUp redisCleanUp;

    @Autowired
    CacheManager cacheManager;

    @MockitoSpyBean
    AccommodationAtomicReader accommodationAtomicReader;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.flushAll();
        apiClient.reset();
    }

    @Test
    void 존재하는_숙소를_조회하면_200과_상세를_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 기대값은 운영의 from(...)이 아니라 저장한 값에서 직접 채운다 (test-guide 참고)
        AccommodationDetailResponse expected = AccommodationDetailResponse.builder()
                .id(accommodation.getId())
                .name(accommodation.getName())
                .type(accommodation.getType())
                .description(accommodation.getDescription())
                .amenities(accommodation.getAmenities())
                .checkInTime(accommodation.getCheckInTime())
                .checkOutTime(accommodation.getCheckOutTime())
                .ratingAverage(accommodation.getRatingAverage())
                .operational(true)
                .address(accommodation.getAddress())
                .images(List.of())
                .build();

        // Act
        ResponseEntity<ApiResponse<AccommodationDetailResponse>> response = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    void 숙소_이미지는_정렬_순서대로_반환하고_대표_이미지_표시와_URL이_맞다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodationWithImages();
        // 기대값은 저장한 이미지를 sortOrder 순으로 정렬해 직접 채운다 (운영의 from(...)을 쓰지 않는다)
        List<ImageResponse> expected = accommodation.getImages().stream()
                .sorted(Comparator.comparingInt(AccommodationImage::getSortOrder))
                .map(image -> ImageResponse.builder()
                        .id(image.getId())
                        .url(image.getUrl())
                        .mainImage(image.isMain())
                        .sortOrder(image.getSortOrder())
                        .build())
                .toList();

        // Act
        ResponseEntity<ApiResponse<AccommodationDetailResponse>> response = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getBody().getData().getImages())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void 존재하지_않는_숙소를_조회하면_404와_ACC_001을_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        long missingId = accommodation.getId() + 1;

        // Act
        ResponseEntity<ApiResponse<Void>> response = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", missingId).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACC-001");
    }

    @Test
    void 처음_조회하면_상세가_캐시에_저장된다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();

        // Act
        accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<AccommodationDetailResponse>>() { }
        );

        // Assert
        assertThat(cacheManager.getCache(RedisKeyNames.ACCOMMODATION_DETAIL).get(accommodation.getId()))
                .isNotNull();
    }

    @Test
    void 같은_숙소를_다시_조회하면_DB를_거치지_않고_캐시된_상세를_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        ResponseEntity<ApiResponse<AccommodationDetailResponse>> first = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Act
        ResponseEntity<ApiResponse<AccommodationDetailResponse>> second = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(second.getBody().getData()).usingRecursiveComparison().isEqualTo(first.getBody().getData());
        verify(accommodationAtomicReader, times(1)).getAccommodationDetailWithLock(accommodation.getId());
    }

    @Test
    void 블룸_필터에_없는_숙소는_DB에_있어도_404와_ACC_001을_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodationOutsideBloomFilter();

        // Act
        ResponseEntity<ApiResponse<Void>> response = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACC-001");
    }
}
