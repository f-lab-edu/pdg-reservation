package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.AccommodationImage;
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
        return generateAccommodation(generateCity(), AccommodationType.HOTEL);
    }

    // 임의 도시 이름을 만든다
    public static String generateCity() {
        return "도시-" + randomSuffix();
    }

    // 도시와 유형을 지정하고 나머지는 임의 값으로 채운 운영 중인 숙소를 만든다 (저장하지 않는다)
    public static Accommodation generateAccommodation(String city, AccommodationType type) {
        return Accommodation.builder()
                .name("숙소-" + randomSuffix())
                .type(type)
                .description("설명-" + UUID.randomUUID())
                .amenities(List.of("WIFI", "PARKING"))
                .checkInTime(LocalTime.of(15, 0))
                .checkOutTime(LocalTime.of(11, 0))
                .isOperational(true)
                .address(new Address(city, "12345", "거리-" + randomSuffix()))
                .build();
    }

    // 이미지 3개를 정렬 순서가 섞인 채(2, 0, 1) 붙인 숙소를 만든다. sortOrder 0이 대표 이미지다
    public static Accommodation generateAccommodationWithImages() {
        Accommodation accommodation = generateAccommodation();
        accommodation.addImage(generateImage(2, false));
        accommodation.addImage(generateImage(0, true));
        accommodation.addImage(generateImage(1, false));
        return accommodation;
    }

    private static AccommodationImage generateImage(int sortOrder, boolean main) {
        return AccommodationImage.builder()
                .url("https://example.com/" + UUID.randomUUID() + ".jpg")
                .isMain(main)
                .sortOrder(sortOrder)
                .build();
    }

    private static String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
