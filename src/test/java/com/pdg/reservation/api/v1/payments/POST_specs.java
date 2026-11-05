package com.pdg.reservation.api.v1.payments;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.LoginSession;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.PaymentFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.payment.enums.PaymentStatus;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.dto.ReservationSearchResponse;
import com.pdg.reservation.reservation.enums.ReservationStatus;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.fixture.PaymentGenerator.generateCardPaymentRequestWithoutCardCompany;
import static com.pdg.reservation.fixture.PaymentGenerator.generateEasyPaymentRequest;
import static com.pdg.reservation.fixture.PaymentGenerator.generatePaymentRequest;
import static com.pdg.reservation.fixture.PaymentGenerator.generatePaymentRequestWithReservationId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.get;
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

    @Test
    void 결제_대기_예약을_올바르게_결제하면_201과_결제를_반환한다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());
        // 기대값은 예약 생성 응답에서 직접 채운다. 결제 id와 결제 시각은 서버가 정하므로 따로 확인하고, PG 승인 번호는 Fake가 정해 준다
        PaymentResponse expected = PaymentResponse.builder()
                .reservationId(reservation.getReservationId())
                .amount(reservation.getTotalPrice())
                .method(PaymentMethod.CARD)
                .pgTransactionNumber("FAKE_TX_1")
                .status(PaymentStatus.PAID)
                .build();

        // Act
        ResponseEntity<ApiResponse<PaymentResponse>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PaymentResponse actual = response.getBody().getData();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("paymentId", "paidAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
        assertThat(actual.getPaymentId()).isNotNull();
        assertThat(actual.getPaidAt()).isNotNull();
        timerAwaiter.awaitTimerDeleted(reservation.getReservationId());
    }

    @Test
    void 결제하면_예약이_CONFIRMED가_된다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());

        // Act
        ResponseEntity<ApiResponse<PaymentResponse>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(searchReservationStatuses()).containsExactly(ReservationStatus.CONFIRMED);
        timerAwaiter.awaitTimerDeleted(reservation.getReservationId());
    }

    @Test
    void 결제하면_결제_대기_타이머가_지워진다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        // 예약을 만들면 결제 대기 타이머가 생긴다
        assertThat(timerAwaiter.timerExists(reservation.getReservationId())).isTrue();
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());

        // Act
        memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<ApiResponse<PaymentResponse>>() { }
        );

        // Assert
        // 결제 알림을 받은 Kafka 소비자가 비동기로 타이머를 지운다. 지워지지 않으면 결제한 예약이 만료되어 취소된다
        timerAwaiter.awaitTimerDeleted(reservation.getReservationId());
    }

    @Test
    void 이미_결제된_예약이면_400과_PAYMENT_003을_반환한다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        paymentFixture.createPayment(reservation.getReservationId(), reservation.getTotalPrice());
        PaymentRequest secondRequest = generatePaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(secondRequest),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_003");
    }

    @Test
    void 결제_금액이_예약_금액보다_적으면_400과_PAYMENT_004를_반환한다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        BigDecimal lowerAmount = reservation.getTotalPrice().subtract(BigDecimal.ONE);
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), lowerAmount);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_004");
    }

    @Test
    void 결제_금액이_예약_금액보다_많아도_400과_PAYMENT_004를_반환한다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        BigDecimal higherAmount = reservation.getTotalPrice().add(BigDecimal.ONE);
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), higherAmount);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_004");
    }

    @Test
    void 카드_결제인데_카드사가_없으면_400과_PAYMENT_002를_반환한다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        PaymentRequest request = generateCardPaymentRequestWithoutCardCompany(
                reservation.getReservationId(), reservation.getTotalPrice());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_002");
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidPaymentRequests")
    void 요청이_유효하지_않으면_400과_COMMON_005를_반환한다(PaymentRequest request) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 결제_금액이_최소_100원이면_결제된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 1박 50원인 객실을 2박 예약하면 총액이 100원이다
        Room room = roomFixture.createRoom(accommodation, 50L);
        roomFixture.createRoomInventories(room, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());

        // Act
        ResponseEntity<ApiResponse<PaymentResponse>> response = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        timerAwaiter.awaitTimerDeleted(reservation.getReservationId());
    }

    @Test
    void PG_승인이_실패하면_502와_PAYMENT_005를_반환하고_예약은_결제_대기로_남아_다시_결제할_수_있다() {
        // Arrange
        ReservationResponse reservation = createPendingReservation();
        PaymentRequest request = generatePaymentRequest(reservation.getReservationId(), reservation.getTotalPrice());
        fakePgClient.failNextConfirm();

        // Act
        ResponseEntity<ApiResponse<Void>> failed = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(failed.getBody().getErrorCode()).isEqualTo("PAYMENT_005");
        assertThat(searchReservationStatuses()).containsExactly(ReservationStatus.PENDING_PAYMENT);
        assertThat(timerAwaiter.timerExists(reservation.getReservationId())).isTrue();
        // 같은 예약을 다시 결제하면 성공한다
        ResponseEntity<ApiResponse<PaymentResponse>> retried = memberFixture.client().exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );
        assertThat(retried.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        timerAwaiter.awaitTimerDeleted(reservation.getReservationId());
    }

    // 기본 사용자로 결제 대기 예약 하나를 만든다 (객실 기본 요금으로 2박)
    private ReservationResponse createPendingReservation() {
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        return reservationFixture.createReservation(room, checkInDate, checkOutDate);
    }

    // 기본 사용자의 예약 검색 결과에서 예약 상태 목록을 돌려준다
    private List<ReservationStatus> searchReservationStatuses() {
        ResponseEntity<ApiResponse<PageResponse<ReservationSearchResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reservations/search").build(),
                new ParameterizedTypeReference<>() { }
        );
        return response.getBody().getData().getContent().stream()
                .map(ReservationSearchResponse::getStatus)
                .toList();
    }
}
