package com.pdg.reservation.common.outbox;

import com.pdg.reservation.common.entity.BaseEntity;
import com.pdg.reservation.common.enums.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 반드시 Kafka로 전달되어야 하는 이벤트를 본 작업과 같은 트랜잭션에 기록해 두는 행(Outbox).
// 스케줄러가 PENDING 행을 Kafka로 보내고 SENT로 바꾼다. 보낸 뒤 SENT 기록 전에 서버가 죽으면 한 번 더 나가므로(적어도 한 번 전달)
// 받는 쪽은 event_log의 event_id로 중복을 막는다.
@Entity
@Table(name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_event_status_id", columnList = "status, id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_outbox_event_event_id", columnNames = "event_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventType eventType;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 100)
    private String messageKey;

    // 보낼 메시지 객체의 클래스 이름과 JSON. 전송할 때 이 클래스로 되살려 Kafka 직렬화기에 넘긴다
    @Column(nullable = false, length = 200)
    private String payloadType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutboxStatus status;

    // 전송을 시도한 횟수(성공한 시도도 센다)
    @Column(nullable = false)
    private int attemptCount;

    private LocalDateTime sentAt;

    public static OutboxEvent create(String eventId, EventType eventType, String topic, String messageKey,
                                     String payloadType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.eventId = eventId;
        event.eventType = eventType;
        event.topic = topic;
        event.messageKey = messageKey;
        event.payloadType = payloadType;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.attemptCount = 0;
        return event;
    }

    // 전송을 한 번 시도했다. 성공하면 SENT로 바꾼다
    public void recordAttempt(boolean sent) {
        this.attemptCount++;
        if (sent) {
            this.status = OutboxStatus.SENT;
            this.sentAt = LocalDateTime.now();
        }
    }
}
