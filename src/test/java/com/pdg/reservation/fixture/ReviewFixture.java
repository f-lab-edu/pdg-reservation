package com.pdg.reservation.fixture;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.review.dto.ReviewCreateRequest;
import com.pdg.reservation.review.dto.ReviewCreateResponse;
import com.pdg.reservation.review.dto.ReviewDeleteResponse;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.RatingAwaiter;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static com.pdg.reservation.fixture.ReviewGenerator.generateReviewCreateRequest;
import static org.springframework.http.RequestEntity.delete;
import static org.springframework.http.RequestEntity.post;

@Component
public class ReviewFixture {

    // 만들 리뷰. authorization이 null이면 기본 사용자로 쓴다
    public record NewReview(UUID reservationId, BigDecimal rating, String authorization) {

        public static NewReview of(UUID reservationId, String rating) {
            return new NewReview(reservationId, new BigDecimal(rating), null);
        }

        public static NewReview of(UUID reservationId, String rating, String authorization) {
            return new NewReview(reservationId, new BigDecimal(rating), authorization);
        }
    }

    private final TestRestTemplate client;
    private final RatingAwaiter ratingAwaiter;

    public ReviewFixture(ApiClient apiClient, RatingAwaiter ratingAwaiter) {
        this.client = apiClient.rest();
        this.ratingAwaiter = ratingAwaiter;
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }

    // 기본 사용자로 리뷰 API를 호출해 리뷰를 만들고 응답을 돌려준다. 숙소 평점 갱신은 기다리지 않는다
    public ReviewCreateResponse createReview(UUID reservationId, BigDecimal rating) {
        ResponseEntity<ApiResponse<ReviewCreateResponse>> response = client.exchange(
                post("/api/v1/reviews").body(generateReviewCreateRequest(reservationId, rating)),
                new ParameterizedTypeReference<>() { }
        );
        return extractCreated(response);
    }

    // 지정한 사용자(Authorization 헤더 값)로 리뷰를 만들고 응답을 돌려준다. 숙소 평점 갱신은 기다리지 않는다
    public ReviewCreateResponse createReview(UUID reservationId, BigDecimal rating, String authorization) {
        ReviewCreateRequest request = generateReviewCreateRequest(reservationId, rating);
        ResponseEntity<ApiResponse<ReviewCreateResponse>> response = client.exchange(
                post("/api/v1/reviews").header(HttpHeaders.AUTHORIZATION, authorization).body(request),
                new ParameterizedTypeReference<>() { }
        );
        return extractCreated(response);
    }

    // 리뷰가 하나도 없는 숙소에 리뷰를 순서대로 만든다. 리뷰마다 Kafka 소비자가 숙소 평점을 바꾼 것을 확인하고 다음으로 넘어간다
    // 그래야 테스트가 끝난 뒤에 늦은 메시지가 남지 않는다. 연속한 두 리뷰 뒤의 평균이 같으면 처리 여부를 확인할 수 없으므로 거절한다
    public List<ReviewCreateResponse> createReviewsAndAwaitRating(Long accommodationId, List<NewReview> newReviews) {
        List<ReviewCreateResponse> created = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal previousAverage = BigDecimal.ZERO;
        for (NewReview newReview : newReviews) {
            sum = sum.add(newReview.rating());
            BigDecimal average = sum.divide(BigDecimal.valueOf(created.size() + 1L), 1, RoundingMode.HALF_UP);
            if (average.compareTo(previousAverage) == 0) {
                throw new IllegalStateException("리뷰 뒤의 평균이 직전과 같아 비동기 처리 완료를 확인할 수 없습니다. 평점을 바꿔 주세요: " + average);
            }
            created.add(newReview.authorization() == null
                    ? createReview(newReview.reservationId(), newReview.rating())
                    : createReview(newReview.reservationId(), newReview.rating(), newReview.authorization()));
            ratingAwaiter.awaitRatingAverage(accommodationId, average);
            previousAverage = average;
        }
        return created;
    }

    // 기본 사용자로 리뷰 삭제 API를 호출하고 응답을 돌려준다. 숙소 평점 갱신은 기다리지 않는다
    public ReviewDeleteResponse deleteReview(Long reviewId) {
        ResponseEntity<ApiResponse<ReviewDeleteResponse>> response = client.exchange(
                delete("/api/v1/reviews/{reviewId}", reviewId).build(),
                new ParameterizedTypeReference<>() { }
        );
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("리뷰 삭제 실패: " + response.getStatusCode());
        }
        return response.getBody().getData();
    }

    private ReviewCreateResponse extractCreated(ResponseEntity<ApiResponse<ReviewCreateResponse>> response) {
        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new IllegalStateException("리뷰 생성 실패: " + response.getStatusCode());
        }
        return response.getBody().getData();
    }
}
