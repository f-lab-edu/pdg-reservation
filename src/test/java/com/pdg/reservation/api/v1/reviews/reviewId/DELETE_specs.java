package com.pdg.reservation.api.v1.reviews.reviewId;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
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
import com.pdg.reservation.review.dto.ReviewDeleteResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RatingAwaiter;
import com.pdg.reservation.support.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.delete;

@DisplayName("DELETE /api/v1/reviews/{reviewId}")
@IntegrationTest
class DELETE_specs {

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
    void 토큰_없이_리뷰를_삭제하면_401과_AUTH_001을_반환한다() {
        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                delete("/api/v1/reviews/{reviewId}", 1L).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }

    @Test
    void 내_리뷰를_삭제하면_200과_삭제_정보를_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateResponse review = reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.0"))).get(0);

        // Act
        ResponseEntity<ApiResponse<ReviewDeleteResponse>> response = memberFixture.client().exchange(
                delete("/api/v1/reviews/{reviewId}", review.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ReviewDeleteResponse actual = response.getBody().getData();
        assertThat(actual.getDeleteReviewId()).isEqualTo(review.getId());
        assertThat(actual.getDeletedAt()).isNotNull();
        // 마지막 리뷰를 지우면 숙소 평점은 0이 된다. 비동기 처리가 끝날 때까지 기다린 뒤 끝낸다
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), BigDecimal.ZERO);
    }

    @Test
    void 존재하지_않는_리뷰를_삭제하면_404와_REVIEW_004를_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateResponse review = reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.0"))).get(0);
        long missingReviewId = review.getId() + 1;

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                delete("/api/v1/reviews/{reviewId}", missingReviewId).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("REVIEW_004");
    }

    @Test
    void 이미_삭제된_리뷰를_삭제하면_400과_REVIEW_005를_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateResponse review = reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.0"))).get(0);
        reviewFixture.deleteReview(review.getId());
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), BigDecimal.ZERO);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                delete("/api/v1/reviews/{reviewId}", review.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("REVIEW_005");
    }

    @Test
    void 남의_리뷰를_삭제하면_403과_REVIEW_006을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        SignupRequest otherMember = memberFixture.createMember();
        LoginSession otherSession = memberFixture.login(otherMember);
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateResponse review = reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.0"))).get(0);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                delete("/api/v1/reviews/{reviewId}", review.getId())
                        .header(HttpHeaders.AUTHORIZATION, otherSession.bearer())
                        .build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getErrorCode()).isEqualTo("REVIEW_006");
    }

    @Test
    void 리뷰를_삭제하면_숙소_평점이_삭제_전_평균으로_되돌아간다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(4));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 2);
        // 평점 4.0, 2.0이면 평균 3.0이다. 둘째를 지우면 4.0으로 돌아온다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "4.0"),
                NewReview.of(reservations.get(1).getReservationId(), "2.0")));

        // Act
        reviewFixture.deleteReview(reviews.get(1).getId());

        // Assert
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), new BigDecimal("4.0"));
    }
}
