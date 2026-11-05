package com.pdg.reservation.api.v1.reviews.my;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.LoginSession;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.ReviewFixture;
import com.pdg.reservation.fixture.ReviewFixture.NewReview;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.review.dto.ReviewCreateResponse;
import com.pdg.reservation.review.dto.ReviewResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RatingAwaiter;
import com.pdg.reservation.support.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/reviews/my")
@IntegrationTest
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
    void 토큰_없이_내_리뷰를_조회하면_401과_AUTH_001을_반환한다() {
        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }

    @Test
    void 내_리뷰를_조회하면_200과_리뷰_목록을_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateResponse review = reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.5"))).get(0);
        // 기대값은 가입 정보, 저장한 숙소·객실, 리뷰 생성 응답에서 직접 채운다. 내 리뷰는 닉네임을 마스킹하지 않는다
        ReviewResponse expected = ReviewResponse.builder()
                .id(review.getId())
                .nickname(member.getNickName())
                .rating(review.getRating())
                .content(review.getContent())
                .accommodationName(accommodation.getName())
                .roomName(room.getName())
                .build();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ReviewResponse> content = response.getBody().getData().getContent();
        assertThat(content)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(expected));
        assertThat(content.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void 다른_회원의_리뷰는_조회되지_않는다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        SignupRequest otherMember = memberFixture.createMember();
        LoginSession otherSession = memberFixture.login(otherMember);
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(4));
        ReservationResponse myReservation = reservationFixture.createCompletedReservation(room, firstDay, firstDay.plusDays(1));
        ReservationResponse otherReservation = reservationFixture.createCompletedReservation(
                room, firstDay.plusDays(2), firstDay.plusDays(3), otherSession.bearer());
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(myReservation.getReservationId(), "4.0"),
                NewReview.of(otherReservation.getReservationId(), "2.0", otherSession.bearer())));
        // 내 리뷰 1건이 한 페이지(size=1)를 채워야 전체 건수 쿼리도 실행된다
        int size = 1;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?size={size}", size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(0).getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 삭제된_리뷰는_조회되지_않는다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(4));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 2);
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "4.0"),
                NewReview.of(reservations.get(1).getReservationId(), "2.0")));
        reviewFixture.deleteReview(reviews.get(1).getId());
        // 삭제도 비동기로 평점을 되돌리므로 반영될 때까지 기다린 뒤 조회한다
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), new BigDecimal("4.0"));
        // 남은 리뷰 1건이 한 페이지(size=1)를 채워야 전체 건수 쿼리도 실행된다
        int size = 1;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?size={size}", size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(0).getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 최소_평점_이상의_리뷰만_조회된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 3);
        // 평점 3.0, 5.0, 1.0 순서로 쓴다. 최소 평점 3은 3.0(경계)과 5.0을 포함한다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "3.0"),
                NewReview.of(reservations.get(1).getReservationId(), "5.0"),
                NewReview.of(reservations.get(2).getReservationId(), "1.0")));
        int minRating = 3;
        // 조건에 맞는 2건이 한 페이지(size=2)를 채워야 전체 건수 쿼리도 실행된다
        int size = 2;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?minRating={minRating}&size={size}", minRating, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(1).getId(), reviews.get(0).getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 정렬을_지정하지_않으면_최근에_쓴_리뷰부터_조회된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 3);
        // 평점 3.0, 5.0, 1.0 순서로 쓴다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "3.0"),
                NewReview.of(reservations.get(1).getReservationId(), "5.0"),
                NewReview.of(reservations.get(2).getReservationId(), "1.0")));

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(2).getId(), reviews.get(1).getId(), reviews.get(0).getId());
    }

    @Test
    void 정렬을_HIGH로_하면_평점이_높은_리뷰부터_조회된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 3);
        // 평점 3.0, 5.0, 1.0 순서로 쓴다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "3.0"),
                NewReview.of(reservations.get(1).getReservationId(), "5.0"),
                NewReview.of(reservations.get(2).getReservationId(), "1.0")));

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?sortType=HIGH").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(1).getId(), reviews.get(0).getId(), reviews.get(2).getId());
    }

    @Test
    void 정렬을_LOW로_하면_평점이_낮은_리뷰부터_조회된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 3);
        // 평점 3.0, 5.0, 1.0 순서로 쓴다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "3.0"),
                NewReview.of(reservations.get(1).getReservationId(), "5.0"),
                NewReview.of(reservations.get(2).getReservationId(), "1.0")));

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?sortType=LOW").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(2).getId(), reviews.get(0).getId(), reviews.get(1).getId());
    }

    @Test
    void 내_리뷰_검색은_첫_페이지를_올바르게_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(10));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 5);
        // 평점 5.0, 4.0, 3.0, 2.0, 1.0 순서로 쓴다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "5.0"),
                NewReview.of(reservations.get(1).getReservationId(), "4.0"),
                NewReview.of(reservations.get(2).getReservationId(), "3.0"),
                NewReview.of(reservations.get(3).getReservationId(), "2.0"),
                NewReview.of(reservations.get(4).getReservationId(), "1.0")));
        int page = 0;
        int size = 2;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?page={page}&size={size}", page, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> pageResponse = response.getBody().getData();
        // 최근에 쓴 리뷰부터 보이므로 가장 나중에 쓴 2건이 첫 페이지다
        assertThat(pageResponse.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(4).getId(), reviews.get(3).getId());
        assertThat(pageResponse.getTotalElements()).isEqualTo(5);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.isLast()).isFalse();
    }

    @Test
    void 내_리뷰_검색은_마지막_페이지를_올바르게_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(10));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 5);
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "5.0"),
                NewReview.of(reservations.get(1).getReservationId(), "4.0"),
                NewReview.of(reservations.get(2).getReservationId(), "3.0"),
                NewReview.of(reservations.get(3).getReservationId(), "2.0"),
                NewReview.of(reservations.get(4).getReservationId(), "1.0")));
        int lastPage = 2;
        int size = 2;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?page={page}&size={size}", lastPage, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> pageResponse = response.getBody().getData();
        // 가장 먼저 쓴 리뷰 1건이 마지막 페이지다
        assertThat(pageResponse.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(0).getId());
        assertThat(pageResponse.getTotalElements()).isEqualTo(5);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.isLast()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidMyReviewSearchUris")
    void 검색_조건이_유효하지_않으면_400과_COMMON_005를_반환한다(URI uri) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                get(uri).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    void 최소_평점이_허용_범위의_경계_값이어도_조회된다(int minRating) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/my?minRating={minRating}", minRating).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
