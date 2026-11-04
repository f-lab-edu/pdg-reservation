package com.pdg.reservation.api.v1.accommodations.accommodationId.rooms;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.room.dto.RoomSearchResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/accommodations/{accommodationId}/rooms")
@IntegrationTest
@Import(TestContainersConfig.class)
class GET_specs {

    @Autowired
    AccommodationFixture accommodationFixture;

    @Autowired
    RoomFixture roomFixture;

    @Autowired
    ApiClient apiClient;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @Autowired
    RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.flushAll();
        apiClient.reset();
    }

    @Test
    void 조건에_맞는_객실을_조회하면_200과_객실_목록을_반환한다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 기대값은 저장한 값에서 직접 채운다. 2박 모두 기본 요금이고 대표 이미지는 없다
        long basePrice = room.getBasePrice().longValue();
        RoomSearchResponse expected = RoomSearchResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .maxCapacity(room.getMaxCapacity())
                .roomLabel(room.getRoomLabel())
                .bedType(room.getBedType())
                .totalPrice(basePrice * 2)
                .avgPrice(basePrice)
                .mainImage(null)
                .build();

        // Act
        ResponseEntity<ApiResponse<PageResponse<RoomSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations/{accommodationId}/rooms?checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getId(),
                                checkInDate,
                                checkOutDate,
                                room.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .usingRecursiveComparison()
                .isEqualTo(List.of(expected));
    }

    @Test
    void 객실이_여러_개이면_id_내림차순으로_반환한다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room first = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        Room second = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        Room third = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);

        // Act
        ResponseEntity<ApiResponse<PageResponse<RoomSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations/{accommodationId}/rooms?checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getId(),
                                checkInDate,
                                checkOutDate,
                                third.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(RoomSearchResponse::getId)
                .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void 총액은_밤_가격의_합이고_평균가는_밤_가격의_평균이다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(3);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 기본 요금과 밤 가격이 모두 다르고, 합·평균·중앙값·최소·최대가 서로 다르게 고른다
        long basePrice = 50_000L;
        List<Long> nightlyPrices = List.of(100_000L, 200_000L, 600_000L);
        long expectedTotalPrice = 900_000L;
        double expectedAvgPrice = 300_000.0;
        Room room = roomFixture.createRoom(accommodation, basePrice);
        roomFixture.createRoomInventoriesWithNightlyPrices(room, checkInDate, nightlyPrices);
        RoomSearchResponse expected = RoomSearchResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .maxCapacity(room.getMaxCapacity())
                .roomLabel(room.getRoomLabel())
                .bedType(room.getBedType())
                .totalPrice(expectedTotalPrice)
                .avgPrice(expectedAvgPrice)
                .mainImage(null)
                .build();

        // Act
        ResponseEntity<ApiResponse<PageResponse<RoomSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations/{accommodationId}/rooms?checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getId(),
                                checkInDate,
                                checkOutDate,
                                room.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .usingRecursiveComparison()
                .isEqualTo(List.of(expected));
    }

    @Test
    void 날짜별_가격이_없는_밤은_객실_기본_요금으로_계산한다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(3);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 둘째 밤은 가격이 없어(null) 기본 요금 50,000원으로 계산된다
        long basePrice = 50_000L;
        List<Long> nightlyPrices = Arrays.asList(100_000L, null, 600_000L);
        long expectedTotalPrice = 750_000L;
        double expectedAvgPrice = 250_000.0;
        Room room = roomFixture.createRoom(accommodation, basePrice);
        roomFixture.createRoomInventoriesWithNightlyPrices(room, checkInDate, nightlyPrices);
        RoomSearchResponse expected = RoomSearchResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .maxCapacity(room.getMaxCapacity())
                .roomLabel(room.getRoomLabel())
                .bedType(room.getBedType())
                .totalPrice(expectedTotalPrice)
                .avgPrice(expectedAvgPrice)
                .mainImage(null)
                .build();

        // Act
        ResponseEntity<ApiResponse<PageResponse<RoomSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations/{accommodationId}/rooms?checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getId(),
                                checkInDate,
                                checkOutDate,
                                room.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .usingRecursiveComparison()
                .isEqualTo(List.of(expected));
    }

    @Test
    void 인원이_객실_최대_인원과_같으면_검색된다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        int capacity = room.getMaxCapacity();

        // Act
        ResponseEntity<ApiResponse<PageResponse<RoomSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations/{accommodationId}/rooms?checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getId(),
                                checkInDate,
                                checkOutDate,
                                capacity).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(RoomSearchResponse::getId)
                .containsExactly(room.getId());
    }

    @Test
    void 인원이_객실_최대_인원을_한_명_초과하면_검색되지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        int capacityOverMax = room.getMaxCapacity() + 1;

        // Act
        ResponseEntity<ApiResponse<PageResponse<RoomSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations/{accommodationId}/rooms?checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getId(),
                                checkInDate,
                                checkOutDate,
                                capacityOverMax).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }
}
