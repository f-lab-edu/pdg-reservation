package com.pdg.reservation.api.v1.reservations.concurrency;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.ConcurrentRunner;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("POST /api/v1/reservations (동시 요청)")
@IntegrationTest
class POST_specs {

    @Autowired
    MemberFixture memberFixture;

    @Autowired
    AccommodationFixture accommodationFixture;

    @Autowired
    RoomFixture roomFixture;

    @Autowired
    ReservationFixture reservationFixture;

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
    void 같은_객실_같은_기간에_동시에_예약하면_한_명만_성공하고_나머지는_거절된다() {
        // Arrange
        int requesterCount = 5;
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        List<String> authorizations = memberFixture.createLoggedInMembers(requesterCount);
        ReservationRequest request = generateReservationRequest(room, checkInDate, checkOutDate);

        // Act
        List<ResponseEntity<ApiResponse<ReservationResponse>>> responses = ConcurrentRunner.runAtOnce(
                authorizations,
                authorization -> reservationFixture.requestReservation(authorization, request)
        );

        // Assert
        assertThat(responses)
                .filteredOn(response -> response.getStatusCode() == HttpStatus.CREATED)
                .hasSize(1);
        assertThat(responses)
                .filteredOn(response -> response.getStatusCode() != HttpStatus.CREATED)
                .hasSize(requesterCount - 1)
                .allSatisfy(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_003");
                });
        assertThat(reservationFixture.countReservations(authorizations)).isEqualTo(1);
    }

    @Test
    void 기간이_겹치는_예약을_동시에_하면_한_명만_성공하고_나머지는_거절된다() {
        // Arrange
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(3));
        List<String> authorizations = memberFixture.createLoggedInMembers(2);
        // 둘째 날 밤이 겹친다: (1~3일), (2~4일)
        List<ReservationAttempt> attempts = List.of(
                new ReservationAttempt(authorizations.get(0), generateReservationRequest(room, firstDay, firstDay.plusDays(2))),
                new ReservationAttempt(authorizations.get(1), generateReservationRequest(room, firstDay.plusDays(1), firstDay.plusDays(3)))
        );

        // Act
        List<ResponseEntity<ApiResponse<ReservationResponse>>> responses = ConcurrentRunner.runAtOnce(
                attempts,
                attempt -> reservationFixture.requestReservation(attempt.authorization(), attempt.request())
        );

        // Assert
        assertThat(responses)
                .filteredOn(response -> response.getStatusCode() == HttpStatus.CREATED)
                .hasSize(1);
        assertThat(responses)
                .filteredOn(response -> response.getStatusCode() != HttpStatus.CREATED)
                .hasSize(1)
                .allSatisfy(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_003");
                });
        assertThat(reservationFixture.countReservations(authorizations)).isEqualTo(1);
    }

    @Test
    void 같은_객실이어도_기간이_겹치지_않으면_동시에_예약해도_모두_성공한다() {
        // Arrange
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(3));
        List<String> authorizations = memberFixture.createLoggedInMembers(2);
        // 하룻밤씩 겹치지 않는다: (1~2일), (3~4일)
        List<ReservationAttempt> attempts = List.of(
                new ReservationAttempt(authorizations.get(0), generateReservationRequest(room, firstDay, firstDay.plusDays(1))),
                new ReservationAttempt(authorizations.get(1), generateReservationRequest(room, firstDay.plusDays(2), firstDay.plusDays(3)))
        );

        // Act
        List<ResponseEntity<ApiResponse<ReservationResponse>>> responses = ConcurrentRunner.runAtOnce(
                attempts,
                attempt -> reservationFixture.requestReservation(attempt.authorization(), attempt.request())
        );

        // Assert
        assertThat(responses)
                .filteredOn(response -> response.getStatusCode() == HttpStatus.CREATED)
                .hasSize(2);
        assertThat(reservationFixture.countReservations(authorizations)).isEqualTo(2);
    }

    @Test
    void 다른_객실이면_같은_기간을_동시에_예약해도_모두_성공한다() {
        // Arrange
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        Room otherRoom = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        List<String> authorizations = memberFixture.createLoggedInMembers(2);
        List<ReservationAttempt> attempts = List.of(
                new ReservationAttempt(authorizations.get(0), generateReservationRequest(room, checkInDate, checkOutDate)),
                new ReservationAttempt(authorizations.get(1), generateReservationRequest(otherRoom, checkInDate, checkOutDate))
        );

        // Act
        List<ResponseEntity<ApiResponse<ReservationResponse>>> responses = ConcurrentRunner.runAtOnce(
                attempts,
                attempt -> reservationFixture.requestReservation(attempt.authorization(), attempt.request())
        );

        // Assert
        assertThat(responses)
                .filteredOn(response -> response.getStatusCode() == HttpStatus.CREATED)
                .hasSize(2);
        assertThat(reservationFixture.countReservations(authorizations)).isEqualTo(2);
    }

    // 요청자(Authorization 헤더 값)와 그가 보낼 예약 요청
    private record ReservationAttempt(String authorization, ReservationRequest request) {
    }
}
