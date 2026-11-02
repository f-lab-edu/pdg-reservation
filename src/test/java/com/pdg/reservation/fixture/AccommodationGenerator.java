package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.Address;
import com.pdg.reservation.accommodation.enums.AccommodationType;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class AccommodationGenerator {

    private AccommodationGenerator() {
    }

    // 임의 값으로 채운 운영 중인 숙소를 만든다 (저장하지 않는다)
    public static Accommodation generateAccommodation() {
        return Accommodation.builder()
                .name("숙소-" + randomSuffix())
                .type(AccommodationType.HOTEL)
                .description("설명-" + UUID.randomUUID())
                .amenities(List.of("WIFI", "PARKING"))
                .checkInTime(LocalTime.of(15, 0))
                .checkOutTime(LocalTime.of(11, 0))
                .isOperational(true)
                .address(new Address("도시-" + randomSuffix(), "12345", "거리-" + randomSuffix()))
                .build();
    }

    private static String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
