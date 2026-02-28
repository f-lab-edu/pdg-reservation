package com.pdg.reservation.payment.client;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.enums.CancelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class MockPgClient implements PgClient {

    @Override
    public String confirm(PaymentRequest request) {
        log.info("결제 승인 요청 시작 - Amount: {}", request.getAmount());

        simulateDelay(500, 1000);

        if (Math.random() < 0.05) {
            log.error("결제 승인 중 네트워크 에러 발생");
            throw new CustomException(ErrorCode.PAYMENT_PG_CONNECTION_ERROR);
        }

        String pgTransactionNumber = "TXNUM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("결제 승인 완료 - pgTransactionNumber: {}", pgTransactionNumber);
        return pgTransactionNumber;
    }

    @Override
    public void cancel(String pgTransactionNumber, CancelType cancelType) {
        log.warn("결제 취소 시작 - 대상 TxNum: {}, 사유: {}", pgTransactionNumber, cancelType.getDescription());

        simulateDelay(200, 500);

        if (Math.random() < 0.03) {
            log.error("결제 취소 에러 발생 - TXNUM_: {}, 취소 타입 - {}", pgTransactionNumber, cancelType.getDescription());
            throw new CustomException(cancelType.getErrorCode());
        }

        log.info("결제 취소 성공 - TxNum: {}", pgTransactionNumber);
    }

    private void simulateDelay(int min, int max) {
        try {
            Thread.sleep((long) (Math.random() * (max - min)) + min);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}