package com.pdg.reservation.common.service;

import com.pdg.reservation.common.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final EventLogRepository eventLogRepository;

    @Transactional
    public boolean isAlreadyProcessed(String eventId, String type, Long aggregateId) {
        int inserted = eventLogRepository.insertIgnore(eventId, type, aggregateId);
        if (inserted == 0) {
            log.info("[IDEMPOTENCY] 중복 이벤트 스킵: {}", eventId);
            return true;
        }
        return false;
    }
}