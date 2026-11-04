package com.pdg.reservation.api.v1.reservations;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.enums.ReservationStatus;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.get;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/reservations")
@IntegrationTest
@Import(TestContainersConfig.class)
class POST_specs {

    @Autowired
    MemberFixture memberFixture;

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

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidReservationRequests")
    void 요청이_유효하지_않으면_400과_COMMON_005를_반환한다(ReservationRequest request) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 예약할_수_없는_객실이면_409와_RESERVE_008을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createNonReservableRoomWithInventories(accommodation, checkInDate, checkOutDate);
        BigDecimal totalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(2));
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate, 1, totalPrice);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_008");
    }

    @Test
    void 운영_중이_아닌_숙소의_객실이면_409와_RESERVE_008을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createNonOperationalAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        BigDecimal totalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(2));
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate, 1, totalPrice);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_008");
    }

    @Test
    void 올바른_요청으로_예약하면_201과_예약을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        int guestCount = 2;
        BigDecimal totalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(2));
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate, guestCount, totalPrice);
        // 기대값은 저장한 값과 보낸 요청에서 직접 채운다. 예약 id와 생성 시각은 서버가 정하므로 따로 확인한다
        ReservationResponse expected = ReservationResponse.builder()
                .accommodationName(accommodation.getName())
                .roomName(room.getName())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .guestCount(guestCount)
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING_PAYMENT)
                .guestName(request.getGuestName())
                .guestPhoneNumber(request.getGuestPhoneNumber())
                .build();

        // Act
        ResponseEntity<ApiResponse<ReservationResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ReservationResponse actual = response.getBody().getData();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("reservationId", "createdAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
        assertThat(actual.getReservationId()).isNotNull();
        assertThat(actual.getCreatedAt()).isNotNull();
    }

    @Test
    void 예약하면_예약한_밤의_재고만_소진된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstNight = LocalDate.now().plusDays(30);
        LocalDate secondNight = firstNight.plusDays(1);
        LocalDate thirdNight = firstNight.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 세 밤의 재고를 만들고 첫째·둘째 밤만 예약한다
        Room room = roomFixture.createRoomWithInventories(accommodation, firstNight, thirdNight.plusDays(1));
        BigDecimal totalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(2));
        ReservationRequest request = generateReservationRequest(room, firstNight, thirdNight, 1, totalPrice);

        // Act
        ResponseEntity<ApiResponse<ReservationResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(searchRoomIds(accommodation, firstNight, room.getMaxCapacity())).isEmpty();
        assertThat(searchRoomIds(accommodation, secondNight, room.getMaxCapacity())).isEmpty();
        assertThat(searchRoomIds(accommodation, thirdNight, room.getMaxCapacity())).containsExactly(room.getId());
    }

    // 하룻밤(해당 날짜 체크인, 다음 날 체크아웃)을 객실 검색 API로 조회해 나온 객실 id를 돌려준다
    private List<Long> searchRoomIds(Accommodation accommodation, LocalDate night, int capacity) {
        ResponseEntity<ApiResponse<PageResponse<RoomSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/accommodations/{accommodationId}/rooms?checkInDate={checkIn}&checkOutDate={checkOut}&capacity={capacity}",
                        accommodation.getId(),
                        night,
                        night.plusDays(1),
                        capacity).build(),
                new ParameterizedTypeReference<>() { }
        );
        return response.getBody().getData().getContent().stream()
                .map(RoomSearchResponse::getId)
                .toList();
    }

    @Test
    void 총액은_밤_가격의_합이다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(3);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 기본 요금과 밤 가격이 모두 다르고, 합·평균·최대가 서로 다르게 고른다
        long basePrice = 50_000L;
        List<Long> nightlyPrices = List.of(100_000L, 200_000L, 600_000L);
        BigDecimal expectedTotalPrice = BigDecimal.valueOf(900_000L);
        Room room = roomFixture.createRoom(accommodation, basePrice);
        roomFixture.createRoomInventoriesWithNightlyPrices(room, checkInDate, nightlyPrices);
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate, 1, expectedTotalPrice);

        // Act
        ResponseEntity<ApiResponse<ReservationResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getTotalPrice()).isEqualByComparingTo(expectedTotalPrice);
    }

    @Test
    void 날짜별_가격이_없는_밤은_객실_기본_요금으로_계산한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(3);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 둘째 밤은 가격이 없어(null) 기본 요금 50,000원으로 계산된다
        long basePrice = 50_000L;
        List<Long> nightlyPrices = Arrays.asList(100_000L, null, 600_000L);
        BigDecimal expectedTotalPrice = BigDecimal.valueOf(750_000L);
        Room room = roomFixture.createRoom(accommodation, basePrice);
        roomFixture.createRoomInventoriesWithNightlyPrices(room, checkInDate, nightlyPrices);
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate, 1, expectedTotalPrice);

        // Act
        ResponseEntity<ApiResponse<ReservationResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getTotalPrice()).isEqualByComparingTo(expectedTotalPrice);
    }

    @Test
    void 요청_금액이_서버가_계산한_금액과_다르면_400과_RESERVE_004를_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 서버가 계산하는 총액(기본 요금 2박)보다 1원 많은 금액을 보낸다
        BigDecimal serverTotalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(2));
        BigDecimal wrongTotalPrice = serverTotalPrice.add(BigDecimal.ONE);
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate, 1, wrongTotalPrice);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_004");
    }

    @Test
    void 요청_금액이_서버가_계산한_금액보다_적어도_400과_RESERVE_004를_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 서버가 계산하는 총액(기본 요금 2박)보다 1원 적은 금액을 보낸다
        BigDecimal serverTotalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(2));
        BigDecimal lowerTotalPrice = serverTotalPrice.subtract(BigDecimal.ONE);
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate, 1, lowerTotalPrice);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_004");
    }
}
