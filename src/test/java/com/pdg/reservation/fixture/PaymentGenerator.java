package com.pdg.reservation.fixture;

import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.enums.CardCompany;
import com.pdg.reservation.payment.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentGenerator {

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("200000");

    private PaymentGenerator() {
    }

    // 예약 id와 금액을 지정한 올바른 카드 결제 요청을 만든다
    public static PaymentRequest generatePaymentRequest(UUID reservationId, BigDecimal amount) {
        return validRequestBuilder().reservationId(reservationId).amount(amount).build();
    }

    // 카드사 없이 간편 결제(카카오페이)로 하는 올바른 결제 요청을 만든다
    public static PaymentRequest generateEasyPaymentRequest(UUID reservationId, BigDecimal amount) {
        return validRequestBuilder()
                .reservationId(reservationId)
                .amount(amount)
                .method(PaymentMethod.KAKAO_PAY)
                .cardCompany(null)
                .build();
    }

    // 카드 결제인데 카드사가 없는 결제 요청을 만든다
    public static PaymentRequest generateCardPaymentRequestWithoutCardCompany(UUID reservationId, BigDecimal amount) {
        return validRequestBuilder().reservationId(reservationId).amount(amount).cardCompany(null).build();
    }

    public static PaymentRequest generatePaymentRequestWithReservationId(UUID reservationId) {
        return validRequestBuilder().reservationId(reservationId).build();
    }

    public static PaymentRequest generatePaymentRequestWithMethod(PaymentMethod method) {
        return validRequestBuilder().method(method).build();
    }

    public static PaymentRequest generatePaymentRequestWithAmount(BigDecimal amount) {
        return validRequestBuilder().amount(amount).build();
    }

    // 저장하지 않은 예약 id와 기본 금액으로 채운, 항목마다 올바른 값을 가진 builder (신한카드 결제)
    private static PaymentRequest.PaymentRequestBuilder validRequestBuilder() {
        return PaymentRequest.builder()
                .reservationId(UUID.randomUUID())
                .cardCompany(CardCompany.SHINHAN)
                .method(PaymentMethod.CARD)
                .amount(DEFAULT_AMOUNT);
    }
}
