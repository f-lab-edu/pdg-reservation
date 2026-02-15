package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.enums.AccommodationType;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.Range;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class AccommodationSearchCondition {

    @NotBlank(message = "도시명은 필수 값입니다.")
    @Size(min = 1, max = 50, message = "도시명은 1자 이상 50자 이하입니다.")
    private String city;

    @NotNull(message = "숙소명 타입은 필수 값입니다.")
    private AccommodationType type;

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
        return checkOutDate.isAfter(checkInDate);
    }
}
