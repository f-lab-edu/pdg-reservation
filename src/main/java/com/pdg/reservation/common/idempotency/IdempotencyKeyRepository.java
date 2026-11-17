package com.pdg.reservation.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    // 이미 있는 키면 넣지 않고 0을 돌려준다. 유니크 위반 예외로 트랜잭션을 망가뜨리지 않으려고 INSERT IGNORE를 쓴다
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO idempotency_key (key_value, created_at, updated_at)
        VALUES (:keyValue, now(), now())
        """, nativeQuery = true)
    int insertIgnore(@Param("keyValue") String keyValue);
}
