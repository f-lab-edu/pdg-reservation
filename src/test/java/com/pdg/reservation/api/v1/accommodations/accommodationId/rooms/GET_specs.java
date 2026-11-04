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

    @Test
    void 숙박하는_밤_중_재고가_등록되지_않은_날이_있으면_검색되지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoom(accommodation);
        // 첫째 밤 재고만 저장한다. 둘째 밤(체크인 다음 날)은 재고 행이 없다
        roomFixture.createRoomInventories(room, checkInDate, checkInDate.plusDays(1));

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
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }

    @Test
    void 숙박하는_밤_중_재고가_소진된_날이_있으면_검색되지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoom(accommodation);
        roomFixture.createRoomInventories(room, checkInDate, checkInDate.plusDays(1));
        // 둘째 밤(체크인 다음 날)은 재고 행은 있지만 재고가 없다
        roomFixture.createRoomInventory(room, checkInDate.plusDays(1), false);

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
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }

    @Test
    void 투숙_기간_밖의_재고는_결과에_영향을_주지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        long basePrice = 50_000L;
        Room room = roomFixture.createRoom(accommodation, basePrice);
        // 투숙하는 두 밤의 가격
        roomFixture.createRoomInventoriesWithNightlyPrices(room, checkInDate, List.of(100_000L, 200_000L));
        // 체크인 전날과 체크아웃 날의 재고는 투숙 기간 밖이다. 가격이 크게 달라서 계산에 섞이면 값이 달라진다
        long outsidePrice = 1_000_000L;
        roomFixture.createRoomInventoriesWithNightlyPrices(room, checkInDate.minusDays(1), List.of(outsidePrice));
        roomFixture.createRoomInventoriesWithNightlyPrices(room, checkOutDate, List.of(outsidePrice));
        long expectedTotalPrice = 300_000L;
        double expectedAvgPrice = 150_000.0;
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
    void 다른_숙소의_객실은_검색되지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Accommodation otherAccommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        roomFixture.createRoomWithInventories(otherAccommodation, checkInDate, checkOutDate);

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
                .extracting(RoomSearchResponse::getId)
                .containsExactly(room.getId());
    }
}
