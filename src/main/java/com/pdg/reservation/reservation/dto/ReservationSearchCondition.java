package com.pdg.reservation.reservation.dto;

import com.pdg.reservation.reservation.enums.ReservationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationSearchCondition {

    private ReservationStatus status;

    @Size(max = 50, message = "객실명은 최대 50자까지 가능합니다.")
    private String roomName;

    @Size(max = 50, message = "숙소명은 최대 50자까지 가능합니다.")
    private String accommodationName;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;

    @AssertTrue(message = "체크아웃 날짜는 체크인 날짜보다 이후여야 합니다.")
    public boolean isValidDateRange() {
        //애노테이션 검사 순서보장하지 않음으로 null은 true로 리턴하고 @NotNull에 위임
        if (checkInDate == null || checkOutDate == null) {
            return true;
        }
        return checkOutDate.isAfter(checkInDate);
    }
}
