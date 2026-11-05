package com.pdg.reservation.api.v1.reservations.cancel;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.LoginSession;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.PaymentFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.payment.enums.PaymentStatus;
import com.pdg.reservation.reservation.dto.ReservationCancelRequest;
import com.pdg.reservation.reservation.dto.ReservationCancelResponse;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.FakePgClient;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.pdg.reservation.fixture.ReservationGenerator.generateCancelReasonOfLength;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationCancelRequest;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationCancelRequestWithCancelReason;
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
    PaymentFixture paymentFixture;

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
    void 토큰_없이_예약을_취소하면_401과_AUTH_001을_반환한다() {
        // Arrange
        ReservationCancelRequest request = generateReservationCancelRequest(1L);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }

    @Test
    void 존재하지_않는_예약을_취소하면_404와_RESERVE_006을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        long missingReservationId = paid.reservation().getReservationId() + 1;
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
        PaidReservation paid = createPaidReservation();
        SignupRequest otherMember = memberFixture.createMember();
        LoginSession otherSession = memberFixture.login(otherMember);
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());

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
        assertThat(fakePgClient.cancelledTransactionNumbers()).isEmpty();
    }

    @Test
    void 결제한_예약을_취소하면_200과_취소_결과를_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());
        // 기대값은 결제 응답에서 직접 채운다. 취소 시각은 서버가 정하므로 따로 확인한다
        ReservationCancelResponse expected = ReservationCancelResponse.builder()
                .paymentId(paid.payment().getPaymentId())
                .reservationId(paid.reservation().getReservationId())
                .amount(paid.payment().getAmount())
                .method(PaymentMethod.CARD)
                .pgTransactionNumber(paid.payment().getPgTransactionNumber())
                .status(PaymentStatus.CANCELED)
                .build();

        // Act
        ResponseEntity<ApiResponse<ReservationCancelResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReservationCancelResponse actual = response.getBody().getData();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("canceledAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
        assertThat(actual.getCanceledAt()).isNotNull();
    }

    @Test
    void 취소하면_예약이_CANCELED가_된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());

        // Act
        ResponseEntity<ApiResponse<ReservationCancelResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservationFixture.searchReservationStatuses()).containsExactly(ReservationStatus.CANCELED);
    }

    @Test
    void 취소하면_결제_때_받은_승인_번호로_PG_취소가_한_번_나간다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());

        // Act
        ResponseEntity<ApiResponse<ReservationCancelResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fakePgClient.cancelledTransactionNumbers())
                .containsExactly(paid.payment().getPgTransactionNumber());
    }

    @Test
    void 취소하면_재고가_복구되어_같은_기간을_다시_예약할_수_있다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());

        // Act
        ResponseEntity<ApiResponse<ReservationCancelResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 예약 API가 201이 아니면 Fixture가 예외를 던진다
        ReservationResponse reReserved = reservationFixture.createReservation(
                paid.room(), paid.checkInDate(), paid.checkOutDate());
        assertThat(reReserved.getReservationId()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50})
    void 취소_사유가_최소_1자와_최대_50자이면_취소된다(int length) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        ReservationCancelRequest request = generateReservationCancelRequestWithCancelReason(
                generateCancelReasonOfLength(length));
        request = ReservationCancelRequest.builder()
                .reservationId(paid.reservation().getReservationId())
                .cancelReason(request.getCancelReason())
                .build();

        // Act
        ResponseEntity<ApiResponse<ReservationCancelResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidReservationCancelRequests")
    void 요청이_유효하지_않으면_400과_COMMON_005를_반환한다(ReservationCancelRequest request) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 결제_전_예약을_취소하면_400과_PAYMENT_006을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse pending = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        ReservationCancelRequest request = generateReservationCancelRequest(pending.getReservationId());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("PAYMENT_006");
        assertThat(fakePgClient.cancelledTransactionNumbers()).isEmpty();
    }

    @Test
    void 이미_취소된_예약을_다시_취소하면_400과_PAYMENT_006을_반환하고_PG_취소는_다시_나가지_않는다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());
        ResponseEntity<ApiResponse<ReservationCancelResponse>> first = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Act
        ResponseEntity<ApiResponse<Void>> second = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(second.getBody().getErrorCode()).isEqualTo("PAYMENT_006");
        assertThat(fakePgClient.cancelledTransactionNumbers()).hasSize(1);
    }

    @Test
    void 체크인이_지난_예약을_취소하면_400과_RESERVE_009를_반환하고_PG_취소는_나가지_않는다() {
        // Arrange
        LocalDate today = LocalDate.now();
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservationStayingFrom(today.minusDays(3), today.minusDays(1));
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_009");
        assertThat(fakePgClient.cancelledTransactionNumbers()).isEmpty();
    }

    @Test
    void 체크인이_오늘인_예약을_취소하면_400과_RESERVE_009를_반환한다() {
        // Arrange
        LocalDate today = LocalDate.now();
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservationStayingFrom(today, today.plusDays(1));
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_009");
    }

    @Test
    void 체크인이_내일인_예약은_취소된다() {
        // Arrange
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservationStayingFrom(tomorrow, tomorrow.plusDays(1));
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());

        // Act
        ResponseEntity<ApiResponse<ReservationCancelResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void PG_취소가_실패하면_502와_PAYMENT_007을_반환하고_예약은_확정으로_남아_다시_취소할_수_있다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        PaidReservation paid = createPaidReservation();
        ReservationCancelRequest request = generateReservationCancelRequest(paid.reservation().getReservationId());
        fakePgClient.failNextCancel();

        // Act
        ResponseEntity<ApiResponse<Void>> failed = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(failed.getBody().getErrorCode()).isEqualTo("PAYMENT_007");
        assertThat(reservationFixture.searchReservationStatuses()).containsExactly(ReservationStatus.CONFIRMED);
        // 같은 예약을 다시 취소하면 성공한다
        ResponseEntity<ApiResponse<ReservationCancelResponse>> retried = memberFixture.client().exchange(
                post("/api/v1/reservations/cancel").body(request),
                new ParameterizedTypeReference<>() { }
        );
        assertThat(retried.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // 결제까지 끝난 예약과 그 예약의 객실, 기간, 결제 결과
    private record PaidReservation(
            Room room, LocalDate checkInDate, LocalDate checkOutDate,
            ReservationResponse reservation, PaymentResponse payment) {
    }

    // 이미 세팅된 기본 사용자로 30일 뒤 2박 예약을 만들어 카드로 결제한다
    private PaidReservation createPaidReservation() {
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        PaymentResponse payment = paymentFixture.createPayment(reservation.getReservationId(), reservation.getTotalPrice());
        return new PaidReservation(room, checkInDate, checkOutDate, reservation, payment);
    }

    // 이미 세팅된 기본 사용자의 결제 완료 예약을 만들고 숙박 기간을 지정한 값으로 바꾼다
    private PaidReservation createPaidReservationStayingFrom(LocalDate stayCheckInDate, LocalDate stayCheckOutDate) {
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createReservationWithStayDates(
                room, checkInDate, checkOutDate, stayCheckInDate, stayCheckOutDate);
        PaymentResponse payment = paymentFixture.createPayment(reservation.getReservationId(), reservation.getTotalPrice());
        return new PaidReservation(room, stayCheckInDate, stayCheckOutDate, reservation, payment);
    }
}
