package com.pdg.reservation.room.entity;

import com.pdg.reservation.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 운영자가 막은 객실의 날짜(수리, 휴업 등). 행이 있는 밤은 예약할 수 없고 검색에서도 제외된다.
@Entity
@Table(name = "room_blocked_date",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_blocked_date_room_date", columnNames = {"room_id", "blocked_date"})
        })
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomBlockedDate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate blockedDate;
}
