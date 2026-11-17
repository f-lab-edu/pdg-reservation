package com.pdg.reservation.common.outbox;

public enum OutboxStatus {
    // 기록만 했고 아직 Kafka로 보내지 않았다
    PENDING,
    // Kafka가 받았다
    SENT
}
