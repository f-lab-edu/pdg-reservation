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
