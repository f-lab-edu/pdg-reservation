package com.pdg.reservation.payment.entity;

import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void 결제_완료된_결제가_취소를_시작하면_CANCELING이_된다() {
        // Arrange
        Payment payment = paymentOf(PaymentStatus.PAID);

        // Act
        payment.updateStatusCanceling();

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELING);
    }

    @Test
    void 결제_완료가_아닌_결제는_취소를_시작할_수_없다() {
        // Arrange
        Payment payment = paymentOf(PaymentStatus.CANCELED);

        // Act, Assert
        assertThatThrownBy(payment::updateStatusCanceling)
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_STATUS));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void 취소가_진행_중인_결제는_취소로_확정된다() {
        // Arrange
        Payment payment = paymentOf(PaymentStatus.CANCELING);

        // Act
        payment.updateStatusCanceled();

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCanceledAt()).isNotNull();
    }

    @Test
    void 취소가_진행_중이_아닌_결제는_취소로_확정할_수_없다() {
        // Arrange
        Payment payment = paymentOf(PaymentStatus.PAID);

        // Act, Assert
        assertThatThrownBy(payment::updateStatusCanceled)
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_STATUS));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void 취소가_진행_중인_결제는_PG가_거절하면_결제_완료로_돌아간다() {
        // Arrange
        Payment payment = paymentOf(PaymentStatus.CANCELING);

        // Act
        payment.revertCanceling();

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void 취소가_진행_중이_아닌_결제는_결제_완료로_되돌릴_수_없다() {
        // Arrange
        Payment payment = paymentOf(PaymentStatus.CANCELED);

        // Act, Assert
        assertThatThrownBy(payment::revertCanceling)
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_STATUS));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    private Payment paymentOf(PaymentStatus status) {
        return Payment.builder().status(status).build();
    }
}
