package com.pdg.reservation.common.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OutboxFacade {

    private final OutboxService outboxService;
    private final TransactionTemplate writeTx;

    public OutboxFacade(OutboxService outboxService, TransactionTemplate writeTransactionTemplate) {
        this.outboxService = outboxService;
        this.writeTx = writeTransactionTemplate;
    }

    public List<Long> findPendingIds() {
        return outboxService.findPendingIds();
    }

    // 한 건을 Kafka로 보내고 결과를 기록한다. 보내는 데 성공했는지를 돌려준다
    // 외부 호출(Kafka)이 있어서 메서드에는 트랜잭션을 두지 않고, DB 구간만 TransactionTemplate으로 묶는다
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean relay(Long outboxId) {
        Optional<OutboxEvent> event = outboxService.find(outboxId);
        if (event.isEmpty()) {
            return true;
        }

        boolean sent = outboxService.send(event.get());
        writeTx.executeWithoutResult(status -> outboxService.recordAttempt(outboxId, sent));
        return sent;
    }
}
