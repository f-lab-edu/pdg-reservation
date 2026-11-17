package com.pdg.reservation.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdg.reservation.common.enums.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// 이벤트를 Outbox에 기록하고, 기록된 이벤트를 Kafka로 보낸다. 전송 순서와 트랜잭션 경계는 OutboxFacade가 정한다
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 본 작업과 같은 트랜잭션에서 호출한다. 본 작업이 롤백되면 이 행도 사라진다
    @Transactional
    public void record(String eventId, EventType eventType, String topic, String messageKey, Object message) {
        outboxRepository.save(OutboxEvent.create(
                eventId, eventType, topic, messageKey, message.getClass().getName(), toJson(message)));
    }

    @Transactional(readOnly = true)
    public List<Long> findPendingIds() {
        return outboxRepository.findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING).stream()
                .map(OutboxEvent::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OutboxEvent> find(Long id) {
        return outboxRepository.findById(id);
    }

    // Kafka로 보내고 받을 때까지 기다린다. 트랜잭션 밖에서 호출한다. 실패하면 false다
    public boolean send(OutboxEvent event) {
        try {
            Object message = objectMapper.readValue(event.getPayload(), Class.forName(event.getPayloadType()));
            kafkaTemplate.send(event.getTopic(), event.getMessageKey(), message)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Outbox] 전송 중 인터럽트 - eventId: {}", event.getEventId(), e);
            return false;
        } catch (Exception e) {
            log.error("[Outbox] 전송 실패 - eventId: {}, attempt: {}", event.getEventId(), event.getAttemptCount() + 1, e);
            //errorAlertService.sendMessengerAlert(...);
            return false;
        }
    }

    // 전송 시도 결과를 기록한다
    @Transactional
    public void recordAttempt(Long id, boolean sent) {
        outboxRepository.findById(id).ifPresent(event -> event.recordAttempt(sent));
    }

    private String toJson(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox 메시지를 JSON으로 바꾸지 못했습니다: " + message.getClass().getName(), e);
        }
    }
}
