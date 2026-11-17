package com.pdg.reservation.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Outbox의 PENDING 행을 주기적으로 Kafka로 보낸다. 한 건당 Facade를 한 번 호출한다(건별로 트랜잭션이 나뉜다)
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxFacade outboxFacade;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:1000}")
    public void relay() {
        List<Long> pendingIds = outboxFacade.findPendingIds();
        for (Long outboxId : pendingIds) {
            // 숙소 id 키로 파티션 순서를 지키려고 첫 실패에서 멈춘다. 나머지는 다음 주기에 이어서 보낸다
            if (!outboxFacade.relay(outboxId)) {
                return;
            }
        }
    }
}
