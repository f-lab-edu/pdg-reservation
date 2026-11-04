package com.pdg.reservation.support;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.common.dto.ApiResponse;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.RequestEntity.get;

// 리뷰를 쓰거나 지우면 Kafka 소비자가 비동기로 숙소 평점을 바꾼다. 숙소 상세 API의 평점이 기대값이 될 때까지 기다린다
@Component
public class RatingAwaiter {

    private static final Duration MAX_WAIT = Duration.ofSeconds(10);

    private final TestRestTemplate client;

    public RatingAwaiter(ApiClient apiClient) {
        this.client = apiClient.rest();
    }

    public void awaitRatingAverage(Long accommodationId, BigDecimal expected) {
        await().atMost(MAX_WAIT).untilAsserted(() -> {
            ResponseEntity<ApiResponse<AccommodationDetailResponse>> response = client.exchange(
                    get("/api/v1/accommodations/{id}", accommodationId).build(),
                    new ParameterizedTypeReference<>() { }
            );
            assertThat(response.getBody().getData().getRatingAverage()).isEqualByComparingTo(expected);
        });
    }
}
