package com.pdg.reservation.room.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class RoomSearchCondition {

    @NotNull(message = "체크인 날짜는 필수 값입니다.")
    @FutureOrPresent(message = "과거 날짜는 선택이 불가능합니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    @NotNull(message = "체크아웃 날짜는 필수 값입니다.")
    @Future(message = "현재 또는 과거 날짜는 선택이 불가능합니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;

    @NotNull(message = "인원수는 필수입니다.")
    @Range(min = 1, max = 100)
    private Integer capacity;

    @AssertTrue(message = "체크아웃 날짜는 체크인 날짜보다 이후여야 합니다.")
    public boolean isValidDateRange() {
        //애노테이션 검사 순서보장하지 않음으로 null은 true로 리턴하고 @NotNull에 위임
        if (checkInDate == null || checkOutDate == null) {
            return true;
        }
        return checkOutDate.isAfter(checkInDate);
    }
}
