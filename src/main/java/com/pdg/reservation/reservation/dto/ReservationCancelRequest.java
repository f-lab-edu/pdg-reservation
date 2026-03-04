package com.pdg.reservation.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationCancelRequest {

    @NotNull(message = "취소할 예약 ID는 필수입니다.")
    private Long reservationId;

    @NotBlank(message = "취소 사유를 입력해야 합니다.")
    @Size(min = 1, max = 50, message = "취소 사유는 1자 이상 50자 이하입니다.")
    private String cancelReason;

}