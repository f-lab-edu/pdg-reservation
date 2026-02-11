package com.pdg.reservation.payment.entity;

import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.reservation.entity.Reservation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Builder
@DynamicInsert
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pgTransactionNumber;

    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Long amount = 0L;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'CARD'")
    @Builder.Default
    private PaymentMethod method = PaymentMethod.CARD;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SHINHAN'")
    @Builder.Default
    private CardCompany cardCompany = CardCompany.SHINHAN;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true )
    private Reservation reservation;

}
