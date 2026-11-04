package com.pdg.reservation.api.v1.reservations.search;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.LoginSession;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.dto.ReservationSearchResponse;
import com.pdg.reservation.reservation.enums.ReservationStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        // 1건이 한 페이지(size=1)를 채워 전체 건수 쿼리도 실행된다
        int size = 1;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search?accommodationName={accommodationName}&size={size}", accommodation.getName(), size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReservationSearchResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReservationSearchResponse::getId)
                .containsExactly(reservation.getReservationId());
        assertThat(page.getTotalElements()).isEqualTo(1);
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

    @Test
    void 내_예약을_검색하면_200과_예약_목록을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        // 기대값은 저장한 값과 예약 생성 응답에서 직접 채운다. 이미지는 없고, 결제 전이라 취소·리뷰 작성은 불가능하다
        ReservationSearchResponse expected = ReservationSearchResponse.builder()
                .id(reservation.getReservationId())
                .accommodationName(accommodation.getName())
                .roomName(room.getName())
                .mainImage(null)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .guestName(reservation.getGuestName())
                .guestPhoneNumber(reservation.getGuestPhoneNumber())
                .status(ReservationStatus.PENDING_PAYMENT)
                .totalPrice(reservation.getTotalPrice())
                .reviewable(false)
                .cancellable(false)
                .build();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ReservationSearchResponse> content = response.getBody().getData().getContent();
        assertThat(content)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(expected));
        assertThat(content.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void 다른_회원의_예약은_검색되지_않는다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        SignupRequest otherMember = memberFixture.createMember();
        LoginSession otherSession = memberFixture.login(otherMember);
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        Room otherRoom = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse myReservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        reservationFixture.createReservation(otherRoom, checkInDate, checkOutDate, otherSession.bearer());
        // 내 예약 1건이 한 페이지(size=1)를 가득 채워야 전체 건수 쿼리도 실행된다
        int size = 1;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search?size={size}", size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReservationSearchResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReservationSearchResponse::getId)
                .containsExactly(myReservation.getReservationId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 예약은_최근에_생성한_순서로_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        // 하룻밤씩 겹치지 않게 순서대로 예약 3건을 만든다
        ReservationResponse first = reservationFixture.createReservation(room, firstDay, firstDay.plusDays(1));
        ReservationResponse second = reservationFixture.createReservation(room, firstDay.plusDays(2), firstDay.plusDays(3));
        ReservationResponse third = reservationFixture.createReservation(room, firstDay.plusDays(4), firstDay.plusDays(5));

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReservationSearchResponse::getId)
                .containsExactly(third.getReservationId(), second.getReservationId(), first.getReservationId());
    }

    @Test
    void 객실명으로_검색하면_객실명이_부분_일치하는_예약만_나온다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        Room otherRoom = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        reservationFixture.createReservation(otherRoom, checkInDate, checkOutDate);
        // 객실명의 일부(접두어 "객실-"를 뺀 임의 부분)만으로 검색한다. 1건이 한 페이지(size=1)를 채워 전체 건수 쿼리도 실행된다
        String roomNamePart = room.getName().substring("객실-".length());
        int size = 1;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search?roomName={roomName}&size={size}", roomNamePart, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReservationSearchResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReservationSearchResponse::getId)
                .containsExactly(reservation.getReservationId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 날짜로_검색하면_체크인과_체크아웃이_범위_안에_있는_예약만_나온다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        // 이어지는 2박씩 3건: [0~2], [2~4], [4~6]일. 가운데 예약이 범위(2~4일)와 시작과 끝이 정확히 같다
        reservationFixture.createReservation(room, firstDay, firstDay.plusDays(2));
        ReservationResponse middle = reservationFixture.createReservation(room, firstDay.plusDays(2), firstDay.plusDays(4));
        reservationFixture.createReservation(room, firstDay.plusDays(4), firstDay.plusDays(6));
        LocalDate startDate = firstDay.plusDays(2);
        LocalDate endDate = firstDay.plusDays(4);

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search?checkInDate={start}&checkOutDate={end}", startDate, endDate).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReservationSearchResponse::getId)
                .containsExactly(middle.getReservationId());
    }
}
