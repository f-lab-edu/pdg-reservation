package com.pdg.reservation.fixture;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequest;
import static org.springframework.http.RequestEntity.post;

@Component
public class ReservationFixture {

    private final TestRestTemplate client;

    public ReservationFixture(ApiClient apiClient) {
        this.client = apiClient.rest();
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }

    // 기본 사용자로 객실을 기본 요금으로 예약한다. 객실의 재고는 기본 요금 그대로여야 한다. 인원은 1명이다
    public ReservationResponse createReservation(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        BigDecimal totalPrice = room.getBasePrice().multiply(BigDecimal.valueOf(nights));
        return createReservation(generateReservationRequest(room, checkInDate, checkOutDate, 1, totalPrice));
    }

    // 기본 사용자로 예약 API를 호출해 예약을 만들고 응답을 돌려준다
    public ReservationResponse createReservation(ReservationRequest request) {
        ResponseEntity<ApiResponse<ReservationResponse>> response = client.exchange(
                post("/api/v1/reservations").body(request),
                new ParameterizedTypeReference<>() { }
        );
        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new IllegalStateException("예약 생성 실패: " + response.getStatusCode());
        }
        return response.getBody().getData();
    }
}
