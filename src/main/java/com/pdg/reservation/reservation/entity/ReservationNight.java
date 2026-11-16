package com.pdg.reservation.reservation.entity;

import com.pdg.reservation.common.entity.BaseEntity;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.price.NightPrice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// 예약이 점유한 하룻밤. (room_id, stay_date) 유니크가 같은 객실 같은 밤의 이중 점유를 DB에서 막는다.
// 예약이 취소되거나 만료되면 행을 지워 그 밤을 다시 예약할 수 있게 한다. 가격은 예약 시점의 스냅샷(등급 할인 전)이다.
@Entity
@Table(name = "reservation_night",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reservation_night_room_date", columnNames = {"room_id", "stay_date"})
        })
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationNight extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate stayDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    public static ReservationNight of(Reservation reservation, Room room, NightPrice nightPrice) {
        return ReservationNight.builder()
                .reservation(reservation)
                .room(room)
                .stayDate(nightPrice.date())
                .price(nightPrice.price())
                .build();
    }
}
