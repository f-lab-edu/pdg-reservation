package com.pdg.reservation.api.v1.reservations.cancel;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.LoginSession;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.reservation.dto.ReservationCancelRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.FakePgClient;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationCancelRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/reservations/cancel")
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
    FakePgClient fakePgClient;

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
        fakePgClient.reset();
        apiClient.reset();
    }

    @Test
    void 존재하지_않는_예약을_취소하면_404와_RESERVE_006을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        long missingReservationId = reservation.getReservationId() + 1;
        ReservationCancelRequest request = generateReservationCancelRequest(missingReservationId);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_006");
    }

    @Test
    void 남의_예약을_취소하면_404와_RESERVE_006을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        SignupRequest otherMember = memberFixture.createMember();
        LoginSession otherSession = memberFixture.login(otherMember);
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 기본 사용자의 예약을 다른 회원이 취소한다
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        ReservationCancelRequest request = generateReservationCancelRequest(reservation.getReservationId());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel")
                        .header(HttpHeaders.AUTHORIZATION, otherSession.bearer())
                        .body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_006");
    }
}
