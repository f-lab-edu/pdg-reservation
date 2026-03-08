package com.pdg.reservation.reservation.entity;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.review.entity.Review;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.common.entity.BaseEntity;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
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

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

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

    @OneToOne(mappedBy = "reservation", fetch = FetchType.LAZY)
    private Payment payment;

    @Version
    private Long version;

    public void updateStatusExpired() {
        if (this.status != ReservationStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.RESERVE_INVALID_STATUS);
        }
        this.status = ReservationStatus.EXPIRED;
    }

    public void updateStatusConfirmed() {
        if (this.status != ReservationStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.RESERVE_INVALID_STATUS);
        }
        this.confirmedAt = LocalDateTime.now();
        this.status = ReservationStatus.CONFIRMED;
    }

    public void updateStatusCanceled() {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.RESERVE_INVALID_STATUS);
        }
        this.canceledAt = LocalDateTime.now();
        this.status = ReservationStatus.CANCELED;
    }

    public void validatePayable(BigDecimal amount) {
        //1. 결제 가능 상태 검증
        if (this.status != ReservationStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_RESERVE_STATUS);
        }

        //2. 결제 가능 가격 검증
        if (this.totalPrice.compareTo(amount) != 0 ) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    public void validateCancelable() {
        //1. 결제 취소 상태 검증
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_CANCEL_STATUS);
        }
    }

    public void validateReviewEligibility(){
        // 리뷰 등록 가능 상태 검증
        /*
        if (this.checkOutDate.isAfter(LocalDate.now())) {
            throw new CustomException(ErrorCode.REVIEW_BEFORE_CHECKOUT);
        }
        */
        if (this.status != ReservationStatus.COMPLETED) {
            throw new CustomException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        }
    }

}
