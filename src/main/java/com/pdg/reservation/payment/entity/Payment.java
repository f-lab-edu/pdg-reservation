package com.pdg.reservation.payment.entity;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.enums.CardCompany;
import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.payment.enums.PaymentStatus;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_resevation_id", columnNames = "reservation_id")
        })
@Getter
@Builder
@DynamicInsert
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //가상의 결제번호 UUID 사용
    @Column(nullable = false)
    private String pgTransactionNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    @ColumnDefault("0.0")
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'CARD'")
    @Builder.Default
    private PaymentMethod method = PaymentMethod.CARD;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CardCompany cardCompany;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PAID'")
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PAID;

    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true )
    private Reservation reservation;

    public void updateStatusCanceled() {
        if (this.status != PaymentStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_STATUS);
        }
        this.canceledAt = LocalDateTime.now();
        this.status = PaymentStatus.CANCELED;
    }



}
