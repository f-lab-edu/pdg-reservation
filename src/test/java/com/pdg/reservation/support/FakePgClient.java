package com.pdg.reservation.support;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.client.PgClient;
import com.pdg.reservation.payment.dto.PaymentRequest;
import com.pdg.reservation.payment.enums.CancelType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// 결정적으로 동작하는 PG 대역. 즉시 응답하고, 실패는 테스트가 켠 스위치로만 일어난다.
// 상태가 있으므로 이 Fake를 쓰는 테스트는 @AfterEach에서 reset()을 부른다.
public class FakePgClient implements PgClient {

    private final AtomicInteger sequence = new AtomicInteger();
    private final List<String> cancelledTransactionNumbers = new CopyOnWriteArrayList<>();
    private volatile boolean failNextConfirm;
    private volatile boolean failNextCancel;

    @Override
    public String confirm(PaymentRequest request) {
        if (failNextConfirm) {
            failNextConfirm = false;
            throw new CustomException(ErrorCode.PAYMENT_PG_CONNECTION_ERROR);
        }
        return "FAKE_TX_" + sequence.incrementAndGet();
    }

    @Override
    public void cancel(String pgTransactionNumber, CancelType cancelType) {
        if (failNextCancel) {
            failNextCancel = false;
            throw new CustomException(cancelType.getErrorCode());
        }
        cancelledTransactionNumbers.add(pgTransactionNumber);
    }

    // 다음 승인 요청 한 번을 PG 통신 오류로 실패시킨다
    public void failNextConfirm() {
        this.failNextConfirm = true;
    }

    // 다음 취소 요청 한 번을 실패시킨다
    public void failNextCancel() {
        this.failNextCancel = true;
    }

    // 취소에 성공한 PG 승인 번호를 요청 순서대로 돌려준다
    public List<String> cancelledTransactionNumbers() {
        return List.copyOf(cancelledTransactionNumbers);
    }

    public void reset() {
        sequence.set(0);
        cancelledTransactionNumbers.clear();
        failNextConfirm = false;
        failNextCancel = false;
    }
}
