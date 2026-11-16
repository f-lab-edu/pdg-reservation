package com.pdg.reservation.room.entity;

import com.pdg.reservation.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// 객실의 날짜별 예외 요금. 기본 요금(room.base_price)과 다른 날짜에만 행을 둔다. 행이 없는 날은 기본 요금이다.
@Entity
@Table(name = "room_price",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_price_room_date", columnNames = {"room_id", "price_date"})
        })
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate priceDate;

    @DecimalMin(value = "0.01", message = "예외 요금은 0보다 커야 합니다.")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
