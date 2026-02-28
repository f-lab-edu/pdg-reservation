package com.pdg.reservation.payment.client;

import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.enums.CancelType;

public interface PgClient {

    // 결제 승인 요청 (성공 시 가상의 PG 트랜잭션 ID 반환)
    String confirm(PaymentRequest request);

    // 결제 취소 요청
    void cancel(String pgTransactionNumber, CancelType cancelType);
}