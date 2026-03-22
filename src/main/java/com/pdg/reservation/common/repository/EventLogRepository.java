package com.pdg.reservation.common.repository;

import com.pdg.reservation.common.entity.EventLog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO event_log (event_id, event_type, aggregate_id, created_at)
        VALUES (:eventId, :eventType, :aggregateId, now())
        """, nativeQuery = true)
    int insertIgnore(@Param("eventId") String eventId,
                     @Param("eventType") String eventType,
                     @Param("aggregateId") Long aggregateId);

    boolean existsByEventId(String eventId);
}