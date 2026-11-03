package com.pdg.reservation.api.v1.accommodations;

import com.pdg.reservation.accommodation.dto.AccommodationSearchResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.RoomFixture;
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

@DisplayName("GET /api/v1/accommodations")
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
    void 조건에_맞는_숙소를_검색하면_200과_숙소_목록을_반환한다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 기대값은 저장한 값에서 직접 채운다. 대표 이미지는 없고, 최저가는 객실 기본 요금이다
        AccommodationSearchResponse expected = new AccommodationSearchResponse(
                accommodation.getId(),
                accommodation.getName(),
                accommodation.getType(),
                accommodation.getAddress().getCity(),
                null
        );
        expected.updateMinPrice(room.getBasePrice().longValue());

        // Act
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getAddress().getCity(),
                                accommodation.getType(),
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
    void 다른_도시의_숙소는_검색되지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation seoul = accommodationFixture.createAccommodation("서울", AccommodationType.HOTEL);
        Accommodation busan = accommodationFixture.createAccommodation("부산", AccommodationType.HOTEL);
        Room seoulRoom = roomFixture.createRoomWithInventories(seoul, checkInDate, checkOutDate);
        roomFixture.createRoomWithInventories(busan, checkInDate, checkOutDate);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                "서울",
                                seoul.getType(),
                                checkInDate,
                                checkOutDate,
                                seoulRoom.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(AccommodationSearchResponse::getId)
                .containsExactly(seoul.getId());
    }

    @Test
    void 다른_유형의_숙소는_검색되지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation hotel = accommodationFixture.createAccommodation("서울", AccommodationType.HOTEL);
        Accommodation motel = accommodationFixture.createAccommodation("서울", AccommodationType.MOTEL);
        Room hotelRoom = roomFixture.createRoomWithInventories(hotel, checkInDate, checkOutDate);
        roomFixture.createRoomWithInventories(motel, checkInDate, checkOutDate);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                "서울",
                                AccommodationType.HOTEL,
                                checkInDate,
                                checkOutDate,
                                hotelRoom.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(AccommodationSearchResponse::getId)
                .containsExactly(hotel.getId());
    }

    @Test
    void 인원이_객실_최대_인원과_같으면_검색된다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getAddress().getCity(),
                                accommodation.getType(),
                                checkInDate,
                                checkOutDate,
                                room.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(AccommodationSearchResponse::getId)
                .containsExactly(accommodation.getId());
    }

    @Test
    void 인원이_객실_최대_인원을_한_명_초과하면_검색되지_않는다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getAddress().getCity(),
                                accommodation.getType(),
                                checkInDate,
                                checkOutDate,
                                room.getMaxCapacity() + 1).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }

    @Test
    void 최저가는_객실별_1박_평균가_중_가장_작은_값이다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 기본 요금은 날짜별 가격과 달라야 한다. 기본 요금을 쓰는 오답과 구별하려는 값이다
        long basePrice = 100000L;
        // 객실 A: 첫째 밤 200,000 / 둘째 밤 140,000
        List<Long> roomAPrices = List.of(200000L, 140000L);
        // 객실 B: 첫째 밤 130,000 / 둘째 밤 150,000
        List<Long> roomBPrices = List.of(130000L, 150000L);
        Room roomA = roomFixture.createRoom(accommodation, basePrice);
        Room roomB = roomFixture.createRoom(accommodation, basePrice);
        roomFixture.createRoomInventoriesWithNightlyPrices(roomA, checkInDate, roomAPrices);
        roomFixture.createRoomInventoriesWithNightlyPrices(roomB, checkInDate, roomBPrices);
        // 1박 평균: A = (200,000 + 140,000) / 2 = 170,000, B = (130,000 + 150,000) / 2 = 140,000
        long roomAAveragePrice = 170000L;
        long roomBAveragePrice = 140000L;
        long expectedMinPrice = Math.min(roomAAveragePrice, roomBAveragePrice);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getAddress().getCity(),
                                accommodation.getType(),
                                checkInDate,
                                checkOutDate,
                                roomA.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(AccommodationSearchResponse::getMinPrice)
                .containsExactly(expectedMinPrice);
    }

    @Test
    void 날짜별_가격이_없는_밤은_객실_기본_요금으로_최저가를_계산한다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 객실 A: 기본 요금 160,000 / 첫째 밤 120,000 / 둘째 밤 가격 없음
        long roomABasePrice = 160000L;
        List<Long> roomAPrices = Arrays.asList(120000L, null);
        // 객실 B: 기본 요금 150,000 / 두 밤 모두 가격 없음
        long roomBBasePrice = 150000L;
        List<Long> roomBPrices = Arrays.asList(null, null);
        Room roomA = roomFixture.createRoom(accommodation, roomABasePrice);
        Room roomB = roomFixture.createRoom(accommodation, roomBBasePrice);
        roomFixture.createRoomInventoriesWithNightlyPrices(roomA, checkInDate, roomAPrices);
        roomFixture.createRoomInventoriesWithNightlyPrices(roomB, checkInDate, roomBPrices);
        // 1박 평균: A = (120,000 + 가격 없는 밤은 기본 요금 160,000) / 2 = 140,000, B = (150,000 + 150,000) / 2 = 150,000
        long roomAAveragePrice = 140000L;
        long roomBAveragePrice = 150000L;
        long expectedMinPrice = Math.min(roomAAveragePrice, roomBAveragePrice);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getAddress().getCity(),
                                accommodation.getType(),
                                checkInDate,
                                checkOutDate,
                                roomA.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(AccommodationSearchResponse::getMinPrice)
                .containsExactly(expectedMinPrice);
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
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getAddress().getCity(),
                                accommodation.getType(),
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
        ResponseEntity<ApiResponse<PageResponse<AccommodationSearchResponse>>> response =
                accommodationFixture.client().exchange(
                        get("/api/v1/accommodations?city={city}&type={type}&checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                                accommodation.getAddress().getCity(),
                                accommodation.getType(),
                                checkInDate,
                                checkOutDate,
                                room.getMaxCapacity()).build(),
                        new ParameterizedTypeReference<>() { }
                );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }
}
