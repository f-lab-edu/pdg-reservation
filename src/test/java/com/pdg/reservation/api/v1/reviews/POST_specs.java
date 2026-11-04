package com.pdg.reservation.api.v1.reviews;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.LoginSession;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.ReviewFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.review.dto.ReviewCreateRequest;
import com.pdg.reservation.review.dto.ReviewCreateResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RatingAwaiter;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.pdg.reservation.fixture.ReviewGenerator.generateReviewCreateRequest;
import static com.pdg.reservation.fixture.ReviewGenerator.generateReviewCreateRequestWithReservationId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.get;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/reviews")
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
    ReservationFixture reservationFixture;

    @Autowired
    ReviewFixture reviewFixture;

    @Autowired
    RatingAwaiter ratingAwaiter;

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
    void 리뷰가_여러_개면_숙소_평점은_리뷰_평점의_평균이다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(4));
        ReservationResponse firstReservation = reservationFixture.createCompletedReservation(room, firstDay, firstDay.plusDays(1));
        ReservationResponse secondReservation = reservationFixture.createCompletedReservation(room, firstDay.plusDays(2), firstDay.plusDays(3));
        reviewFixture.createReview(firstReservation.getReservationId(), new BigDecimal("4.0"));
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), new BigDecimal("4.0"));

        // Act
        reviewFixture.createReview(secondReservation.getReservationId(), new BigDecimal("2.0"));

        // Assert
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), new BigDecimal("3.0"));
    }

    @Test
    void 토큰_없이_리뷰를_쓰면_401과_AUTH_001을_반환한다() {
        // Arrange
        ReviewCreateRequest request = generateReviewCreateRequestWithReservationId(1L);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }

    @Test
    void 이용_완료_예약에_리뷰를_쓰면_201과_리뷰를_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        BigDecimal rating = new BigDecimal("4.5");
        ReviewCreateRequest request = generateReviewCreateRequest(reservation.getReservationId(), rating);
        // 기대값은 가입 정보와 보낸 요청에서 직접 채운다. 리뷰 id와 작성 시각은 서버가 정하므로 따로 확인한다
        ReviewCreateResponse expected = ReviewCreateResponse.builder()
                .nickName(member.getNickName())
                .rating(rating)
                .content(request.getContent())
                .build();

        // Act
        ResponseEntity<ApiResponse<ReviewCreateResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ReviewCreateResponse actual = response.getBody().getData();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getCreatedAt()).isNotNull();
        // 비동기로 숙소 평점이 갱신되므로 반영될 때까지 기다린 뒤 끝낸다
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), rating);
    }

    @Test
    void 이용_완료_상태가_아닌_예약이면_400과_REVIEW_001을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 결제 대기 상태의 예약이다
        ReservationResponse reservation = reservationFixture.createReservation(room, checkInDate, checkOutDate);
        ReviewCreateRequest request = generateReviewCreateRequest(reservation.getReservationId(), new BigDecimal("4.0"));

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("REVIEW_001");
    }

    @Test
    void 이미_리뷰가_있는_예약이면_400과_REVIEW_002를_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        BigDecimal rating = new BigDecimal("4.0");
        reviewFixture.createReview(reservation.getReservationId(), rating);
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), rating);
        ReviewCreateRequest secondRequest = generateReviewCreateRequest(reservation.getReservationId(), rating);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").body(secondRequest),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("REVIEW_002");
    }

    @Test
    void 남의_예약에_리뷰를_쓰면_404와_RESERVE_006을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        SignupRequest otherMember = memberFixture.createMember();
        LoginSession otherSession = memberFixture.login(otherMember);
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        // 기본 사용자의 이용 완료 예약에 다른 회원이 리뷰를 쓴다
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateRequest request = generateReviewCreateRequest(reservation.getReservationId(), new BigDecimal("4.0"));

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").header(HttpHeaders.AUTHORIZATION, otherSession.bearer()).body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_006");
    }

    @Test
    void 존재하지_않는_예약에_리뷰를_쓰면_404와_RESERVE_006을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        long missingReservationId = reservation.getReservationId() + 1;
        ReviewCreateRequest request = generateReviewCreateRequest(missingReservationId, new BigDecimal("4.0"));

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESERVE_006");
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidReviewCreateRequests")
    void 요청이_유효하지_않으면_400과_COMMON_005를_반환한다(ReviewCreateRequest request) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#validReviewBoundaryInputs")
    void 평점과_내용이_허용_범위의_경계_값이어도_작성된다(BigDecimal rating, String content) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateRequest request = generateReviewCreateRequest(reservation.getReservationId(), rating, content);

        // Act
        ResponseEntity<ApiResponse<ReviewCreateResponse>> response = memberFixture.client().exchange(
                post("/api/v1/reviews").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // 비동기로 숙소 평점이 갱신되므로 반영될 때까지 기다린 뒤 끝낸다
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), rating);
    }

    @Test
    void 리뷰를_쓰면_숙소_평점이_갱신된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        BigDecimal rating = new BigDecimal("4.5");

        // Act
        reviewFixture.createReview(reservation.getReservationId(), rating);

        // Assert
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), rating);
    }

    @Test
    void 숙소_상세를_먼저_조회해_캐시에_담아_두어도_리뷰를_쓰면_새_평점이_보인다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        BigDecimal rating = new BigDecimal("4.5");
        // 리뷰를 쓰기 전의 상세(평점 0)가 캐시에 담긴다
        memberFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<AccommodationDetailResponse>>() { }
        );

        // Act
        reviewFixture.createReview(reservation.getReservationId(), rating);

        // Assert
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), rating);
    }
}
