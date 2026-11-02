package com.pdg.reservation.api.v1.accommodations.id;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/accommodations/{id}")
@IntegrationTest
@Import(TestContainersConfig.class)
class GET_specs {

    @Autowired
    AccommodationFixture accommodationFixture;

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
    void 존재하는_숙소를_조회하면_200과_상세를_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();

        // Act
        ResponseEntity<ApiResponse<AccommodationDetailResponse>> response = accommodationFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AccommodationDetailResponse detail = response.getBody().getData();
        assertSoftly(softly -> {
            softly.assertThat(detail.getId()).isEqualTo(accommodation.getId());
            softly.assertThat(detail.getName()).isEqualTo(accommodation.getName());
            softly.assertThat(detail.getType()).isEqualTo(accommodation.getType());
            softly.assertThat(detail.getDescription()).isEqualTo(accommodation.getDescription());
            softly.assertThat(detail.getAmenities()).isEqualTo(accommodation.getAmenities());
            softly.assertThat(detail.isOperational()).isTrue();
            softly.assertThat(detail.getAddress()).isEqualTo(accommodation.getAddress());
        });
    }
}
