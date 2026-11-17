package com.pdg.reservation.common.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyKeyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    // 키를 선점한다. 처음 보는 키면 true, 이미 쓴 키면 false다.
    // 호출하는 쪽의 트랜잭션과 무관하게 이 메서드가 끝나면 바로 커밋되도록 외부 호출(PG)보다 먼저 호출한다
    @Transactional
    public boolean claim(String keyValue) {
        return idempotencyKeyRepository.insertIgnore(keyValue) == 1;
    }
}
