package com.pdg.reservation.fixture;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.dto.PaymentResponse;
import com.pdg.reservation.payment.repository.PaymentRepository;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.ReservationTimerAwaiter;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.pdg.reservation.fixture.PaymentGenerator.generatePaymentRequest;
import static org.springframework.http.RequestEntity.post;

@Component
public class PaymentFixture {

    private final TestRestTemplate client;
    private final ReservationTimerAwaiter timerAwaiter;
    private final PaymentRepository paymentRepository;

    public PaymentFixture(ApiClient apiClient, ReservationTimerAwaiter timerAwaiter, PaymentRepository paymentRepository) {
        this.client = apiClient.rest();
        this.timerAwaiter = timerAwaiter;
        this.paymentRepository = paymentRepository;
    }

    // 저장된 결제 행의 개수. API로는 보이지 않는 결과(롤백 여부)를 확인할 때 쓴다
    public long countPayments() {
        return paymentRepository.count();
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }

    // 기본 사용자가 예약을 카드로 결제하고, Kafka 소비자가 결제 대기 타이머를 지울 때까지 기다린 뒤 응답을 돌려준다
    // 소비가 끝나기 전에 테스트가 끝나 다음 테스트로 넘어가지 않게 하려는 것이다
    public PaymentResponse createPayment(Long reservationId, BigDecimal amount) {
        PaymentRequest request = generatePaymentRequest(reservationId, amount);
        ResponseEntity<ApiResponse<PaymentResponse>> response = client.exchange(
                post("/api/v1/payments").body(request),
                new ParameterizedTypeReference<>() { }
        );
        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new IllegalStateException("결제 실패: " + response.getStatusCode());
        }
        timerAwaiter.awaitTimerDeleted(reservationId);
        return response.getBody().getData();
    }
}
