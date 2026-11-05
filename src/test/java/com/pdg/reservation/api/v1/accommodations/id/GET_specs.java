package com.pdg.reservation.api.v1.accommodations.id;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.dto.ImageResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.AccommodationImage;
import com.pdg.kafka.event.ReviewKafkaEvent;
import com.pdg.reservation.accommodation.service.AccommodationAtomicReader;
import com.pdg.reservation.accommodation.service.AccommodationCacheGenerationService;
import com.pdg.reservation.accommodation.service.AccommodationRatingService;
import com.pdg.reservation.common.enums.EventType;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/accommodations/{id}")
@IntegrationTest
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
    AccommodationRatingService accommodationRatingService;

    @MockitoSpyBean
    AccommodationAtomicReader accommodationAtomicReader;

    @MockitoSpyBean
    AccommodationCacheGenerationService accommodationCacheGenerationService;

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
        verify(accommodationAtomicReader, times(1)).getAccommodationDetailWithLock(eq(accommodation.getId()), anyInt());
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

    @Test
    void 평점이_갱신되는_도중에_끝난_조회가_옛_평점을_캐시에_남기지_않는다() throws Exception {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        CountDownLatch dbRead = new CountDownLatch(1);
        CountDownLatch ratingUpdated = new CountDownLatch(1);
        // 조회가 DB에서 옛 평점(0.0)을 읽은 뒤에는 평점 갱신이 끝날 때까지 응답과 캐시 저장을 미룬다
        doAnswer(invocation -> {
            Object detail = invocation.callRealMethod();
            dbRead.countDown();
            ratingUpdated.await(5, TimeUnit.SECONDS);
            return detail;
        }).when(accommodationAtomicReader).getAccommodationDetailWithLock(eq(accommodation.getId()), anyInt());
        BigDecimal newRating = new BigDecimal("4.0");
        ReviewKafkaEvent reviewCreated = new ReviewKafkaEvent(
                UUID.randomUUID().toString(), accommodation.getId(), 1L, newRating,
                EventType.REVIEW_CREATED.name(), LocalDateTime.now());

        // Act
        // 조회 하나가 DB를 읽은 직후 멈춰 있는 동안 소비자가 평점을 갱신하고 캐시를 정리한다
        CompletableFuture<ResponseEntity<ApiResponse<AccommodationDetailResponse>>> interruptedRead =
                CompletableFuture.supplyAsync(() -> accommodationFixture.client().exchange(
                        get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                        new ParameterizedTypeReference<>() { }
                ));
        dbRead.await(5, TimeUnit.SECONDS);
        accommodationRatingService.updateAccommodationRating(reviewCreated);
        ratingUpdated.countDown();
        interruptedRead.get(5, TimeUnit.SECONDS);
        ResponseEntity<ApiResponse<AccommodationDetailResponse>> response = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getBody().getData().getRatingAverage()).isEqualByComparingTo(newRating);
    }

    @Test
    void 평점_갱신이_커밋되기_전에_들어온_조회가_옛_평점을_새_세대의_캐시에_남기지_않는다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        BigDecimal newRating = new BigDecimal("4.0");
        ReviewKafkaEvent reviewCreated = new ReviewKafkaEvent(
                UUID.randomUUID().toString(), accommodation.getId(), 1L, newRating,
                EventType.REVIEW_CREATED.name(), LocalDateTime.now());
        // 세대가 올라간 바로 그 순간에 상세를 한 번 조회한다.
        // 트랜잭션이 커밋되기 전에 세대를 올리면 이 조회는 아직 보이지 않는 새 평점 대신 옛 평점(0.0)을 읽는다
        doAnswer(invocation -> {
            invocation.callRealMethod();
            accommodationFixture.client().exchange(
                    get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                    new ParameterizedTypeReference<ApiResponse<AccommodationDetailResponse>>() { }
            );
            return null;
        }).when(accommodationCacheGenerationService).increaseGeneration(eq(accommodation.getId()));

        // Act
        accommodationRatingService.updateAccommodationRating(reviewCreated);
        ResponseEntity<ApiResponse<AccommodationDetailResponse>> response = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getBody().getData().getRatingAverage()).isEqualByComparingTo(newRating);
    }
}
