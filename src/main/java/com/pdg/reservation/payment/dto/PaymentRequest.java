package com.pdg.reservation.payment.dto;


import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.enums.CardCompany;
import com.pdg.reservation.payment.enums.PaymentMethod;
import com.pdg.reservation.reservation.entity.Reservation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class PaymentRequest {

    @NotNull(message = "예약 ID는 필수입니다.")
    private Long reservationId;

    private CardCompany cardCompany;

    @NotNull(message = "결제 수단은 필수입니다.")
    private PaymentMethod method;

    @NotNull(message = "결제 금액은 필수입니다.")
    @Min(value=100, message = "결제 최소 금액은 100원 이상이어야 합니다.")
    private BigDecimal amount;

    public void validatePayMethodPolicy(){
        if(this.method.isCardInfoRequired() && cardCompany == null){
            throw new CustomException(ErrorCode.PAYMENT_MISSING_CARD_COMPANY);
        }
    }

    public Payment toEntity(Reservation reservation, String pgTransactionNumber){
        return  Payment.builder()
                .amount(this.amount)
                .cardCompany(this.cardCompany)
                .method(this.method)
                .paidAt(LocalDateTime.now())
                .reservation(reservation)
                .pgTransactionNumber(pgTransactionNumber)
                .build();
    }

}
