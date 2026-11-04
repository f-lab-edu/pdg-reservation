package com.pdg.reservation.api.v1.reservations.search;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.dto.ReservationSearchResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/reservations/search")
@IntegrationTest
@Import(TestContainersConfig.class)
class GET_specs {

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
    void 토큰_없이_예약을_검색하면_401과_AUTH_001을_반환한다() {
        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }

    @Test
    void 숙소명으로_검색하면_숙소명이_부분_일치하는_예약만_나온다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Accommodation otherAccommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        Room otherRoom = roomFixture.createRoomWithInventories(otherAccommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        reservationFixture.createReservation(otherRoom, checkInDate, checkOutDate);

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search?accommodationName={accommodationName}", accommodation.getName()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReservationSearchResponse::getId)
                .containsExactly(reservation.getReservationId());
    }

    @Test
    void 조건으로_걸러진_예약의_수가_전체_건수로_나온다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        // 하룻밤씩 겹치지 않게 예약 3건을 만든다
        ReservationResponse first = reservationFixture.createReservation(room, firstDay, firstDay.plusDays(1));
        ReservationResponse second = reservationFixture.createReservation(room, firstDay.plusDays(2), firstDay.plusDays(3));
        reservationFixture.createReservation(room, firstDay.plusDays(4), firstDay.plusDays(5));
        // 날짜 조건에는 앞의 2건만 들어맞는다. 목록이 한 페이지(size=2)를 가득 채워도 전체 건수는 2여야 한다
        LocalDate startDate = firstDay;
        LocalDate endDate = firstDay.plusDays(3);
        int size = 2;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search?checkInDate={start}&checkOutDate={end}&page=0&size={size}",
                        startDate, endDate, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReservationSearchResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReservationSearchResponse::getId)
                .containsExactlyInAnyOrder(first.getReservationId(), second.getReservationId());
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.isLast()).isTrue();
    }
}
