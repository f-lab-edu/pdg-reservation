package com.pdg.reservation.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // 아직 보내지 못한 행을 기록한 순서대로
    List<OutboxEvent> findTop50ByStatusOrderByIdAsc(OutboxStatus status);
}
