package com.pdg.reservation.api.v1.payments;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.LoginSession;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.PaymentFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.FakePgClient;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import com.pdg.reservation.support.ReservationTimerAwaiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static com.pdg.reservation.fixture.PaymentGenerator.generateEasyPaymentRequest;
import static com.pdg.reservation.fixture.PaymentGenerator.generatePaymentRequest;
import static com.pdg.reservation.fixture.PaymentGenerator.generatePaymentRequestWithReservationId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/payments")
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
    PaymentFixture paymentFixture;

    @Autowired
    FakePgClient fakePgClient;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ReservationTimerAwaiter timerAwaiter;

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
    void 토큰_없이_결제하면_401과_AUTH_001을_반환한다() {
        // Arrange
        PaymentRequest request = generatePaymentRequestWithReservationId(1L);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }

    @Test
    void 존재하지_않는_예약을_결제하면_404와_PAYMENT_001을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        long missingReservationId = reservation.getReservationId() + 1;
        PaymentRequest request = generatePaymentRequest(missingReservationId, reservation.getTotalPrice());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_001");
    }

    @Test
    void 남의_예약을_결제하면_404와_PAYMENT_001을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        SignupRequest otherMember = memberFixture.createMember();
        LoginSession otherSession = memberFixture.login(otherMember);
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 기본 사용자의 예약을 다른 회원이 결제한다
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").header(HttpHeaders.AUTHORIZATION, otherSession.bearer()).body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_001");
    }

    @Test
    void 카드사_없이_간편결제를_하면_201이고_카드사는_저장되지_않는다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        PaymentRequest request = generateEasyPaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());

        // Act
        ResponseEntity<ApiResponse<PaymentResponse>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // 카드사는 응답에 나오지 않아 DB를 직접 읽는다. 간편결제는 카드사가 없으므로 비어 있어야 한다
        String storedCardCompany = jdbcTemplate.queryForObject(
                "SELECT card_company FROM payment WHERE reservation_id = ?", String.class, reservation.getReservationId());
        assertThat(storedCardCompany).isNull();
        // 비동기로 결제 대기 타이머가 지워지므로 끝날 때까지 기다린 뒤 끝낸다
        timerAwaiter.awaitTimerDeleted(reservation.getReservationId());
    }
}
