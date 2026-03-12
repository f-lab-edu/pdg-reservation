package com.pdg.reservation.common.entity;

import com.pdg.reservation.common.enums.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "event_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_id", columnNames = "event_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EventLog(String eventId, EventType eventType, Long aggregateId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.createdAt = LocalDateTime.now();
    }
}