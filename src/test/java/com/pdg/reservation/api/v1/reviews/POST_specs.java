package com.pdg.reservation.api.v1.reviews;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.ReviewFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.reservation.dto.ReservationResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
