package com.pdg.reservation.reservation.entity;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void 확정된_예약이_취소를_시작하면_CANCELING이_된다() {
        // Arrange
        Reservation reservation = reservationOf(ReservationStatus.CONFIRMED);

        // Act
        reservation.updateStatusCanceling();

        // Assert
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELING);
    }

    @Test
    void 확정이_아닌_예약은_취소를_시작할_수_없다() {
        // Arrange
        Reservation reservation = reservationOf(ReservationStatus.PENDING_PAYMENT);

        // Act, Assert
        assertThatThrownBy(reservation::updateStatusCanceling)
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVE_INVALID_STATUS));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
    }

    @Test
    void 취소가_진행_중인_예약은_취소로_확정된다() {
        // Arrange
        Reservation reservation = reservationOf(ReservationStatus.CANCELING);

        // Act
        reservation.updateStatusCanceled();

        // Assert
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(reservation.getCanceledAt()).isNotNull();
    }

    @Test
    void 취소가_진행_중이_아닌_예약은_취소로_확정할_수_없다() {
        // Arrange
        Reservation reservation = reservationOf(ReservationStatus.CONFIRMED);

        // Act, Assert
        assertThatThrownBy(reservation::updateStatusCanceled)
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVE_INVALID_STATUS));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void 취소가_진행_중인_예약은_PG가_거절하면_확정으로_돌아간다() {
        // Arrange
        Reservation reservation = reservationOf(ReservationStatus.CANCELING);

        // Act
        reservation.revertCanceling();

        // Assert
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void 취소가_진행_중이_아닌_예약은_확정으로_되돌릴_수_없다() {
        // Arrange
        Reservation reservation = reservationOf(ReservationStatus.CANCELED);

        // Act, Assert
        assertThatThrownBy(reservation::revertCanceling)
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVE_INVALID_STATUS));
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
    }

    private Reservation reservationOf(ReservationStatus status) {
        return Reservation.builder().status(status).build();
    }
}
