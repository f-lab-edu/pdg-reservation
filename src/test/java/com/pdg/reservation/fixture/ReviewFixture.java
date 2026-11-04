package com.pdg.reservation.fixture;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.review.dto.ReviewCreateRequest;
import com.pdg.reservation.review.dto.ReviewCreateResponse;
import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.pdg.reservation.fixture.ReviewGenerator.generateReviewCreateRequest;
import static org.springframework.http.RequestEntity.post;

@Component
public class ReviewFixture {

    private final TestRestTemplate client;

    public ReviewFixture(ApiClient apiClient) {
        this.client = apiClient.rest();
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }

    // 기본 사용자로 리뷰 API를 호출해 리뷰를 만들고 응답을 돌려준다
    public ReviewCreateResponse createReview(Long reservationId, BigDecimal rating) {
        ResponseEntity<ApiResponse<ReviewCreateResponse>> response = client.exchange(
                post("/api/v1/reviews").body(generateReviewCreateRequest(reservationId, rating)),
                new ParameterizedTypeReference<>() { }
        );
        return extractCreated(response);
    }

    // 지정한 사용자(Authorization 헤더 값)로 리뷰를 만들고 응답을 돌려준다
    public ReviewCreateResponse createReview(Long reservationId, BigDecimal rating, String authorization) {
        ReviewCreateRequest request = generateReviewCreateRequest(reservationId, rating);
        ResponseEntity<ApiResponse<ReviewCreateResponse>> response = client.exchange(
                post("/api/v1/reviews").header(HttpHeaders.AUTHORIZATION, authorization).body(request),
                new ParameterizedTypeReference<>() { }
        );
        return extractCreated(response);
    }

    private ReviewCreateResponse extractCreated(ResponseEntity<ApiResponse<ReviewCreateResponse>> response) {
        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new IllegalStateException("리뷰 생성 실패: " + response.getStatusCode());
        }
        return response.getBody().getData();
    }
}
