package com.pdg.reservation.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.enums.ReservationStatus;
import com.pdg.reservation.room.entity.Room;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationRequest {

    @NotNull(message = "객실 ID는 필수 값입니다.")
    Long roomId;

    @NotNull(message = "체크인 날짜는 필수 값입니다.")
    @FutureOrPresent(message = "과거 날짜는 선택이 불가능합니다.")
    LocalDate checkInDate;

    @NotNull(message = "체크아웃 날짜는 필수 값입니다.")
    @Future(message = "현재 또는 과거 날짜는 선택이 불가능합니다.")
    LocalDate checkOutDate;

    @NotNull(message = "인원수는 필수입니다.")
    @Range(min = 1, max = 100)
    Integer guestCount;

    @NotBlank(message = "예약자 이름은 필수입니다.")
    @Size(min = 1, max = 50)
    String guestName;

    @NotBlank(message = "예약자 연락처는 필수입니다.")
    @Pattern(regexp = "^\\d{2,3}\\d{3,4}\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.") // 💡 형식 검증 추가
    String guestPhoneNumber;

    @NotNull(message = "최종 결제 금액은 필수입니다.")
    BigDecimal totalPrice;

    @AssertTrue(message = "체크아웃 날짜는 체크인 날짜보다 이후여야 합니다.")
    public boolean isValidDateRange() {
        //애노테이션 검사 순서보장하지 않음으로 null은 true로 리턴하고 @NotNull에 위임
        if (checkInDate == null || checkOutDate == null) {
            return true;
        }
        return checkOutDate.isAfter(checkInDate);
    }

    public Reservation toEntity(Member member, Room room) {
        return Reservation.builder()
                .checkInDate(this.checkInDate)
                .checkOutDate(this.checkOutDate)
                .guestCount(this.guestCount)
                .totalPrice(this.totalPrice)
                .guestName(this.guestName)
                .guestPhoneNumber(this.guestPhoneNumber)
                .status(ReservationStatus.PENDING_PAYMENT)
                .member(member)
                .accommodation(room.getAccommodation())
                .room(room)
                .build();
    }


}
