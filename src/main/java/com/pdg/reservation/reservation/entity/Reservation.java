package com.pdg.reservation.reservation.entity;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.Room;
import com.pdg.reservation.common.entity.BaseEntity;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="reservation")
@Getter
@Builder
@DynamicInsert
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    @ColumnDefault("1")
    @Builder.Default
    private int guestCount = 1;

    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private long totalPrice = 0;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING_PAYMENT'")
    @Builder.Default
    private ReservationStatus status =  ReservationStatus.PENDING_PAYMENT;

    @Column(nullable = false, length = 50)
    private String guestName;

    @Column(nullable = false, length = 11)
    private String guestPhoneNumber;

    @Column
    private LocalDateTime confirmedAt;

    @Column
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

}
